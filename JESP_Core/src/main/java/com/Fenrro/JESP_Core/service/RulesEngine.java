package com.Fenrro.JESP_Core.service;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import com.Fenrro.JESP_Core.model.LegacyRulesParser;
import com.Fenrro.JESP_Core.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RulesEngine {

    private final RuleRepository ruleRepository;
    private final DeviceRegistry deviceRegistry;
    private final HistoryService historyService;
    private final String rulesFile;
    private final long sensorStalenessSeconds;
    private final boolean staleFailsafeOff;

    public RulesEngine(RuleRepository ruleRepository,
                       DeviceRegistry deviceRegistry,
                       HistoryService historyService,
                       @Value("${jesp.rules-file}") String rulesFile,
                       @Value("${jesp.sensor-staleness-seconds:300}") long sensorStalenessSeconds,
                       @Value("${jesp.stale-failsafe:OFF}") String staleFailsafe) {
        this.ruleRepository = ruleRepository;
        this.deviceRegistry = deviceRegistry;
        this.historyService = historyService;
        this.rulesFile = rulesFile;
        this.sensorStalenessSeconds = sensorStalenessSeconds;
        this.staleFailsafeOff = "OFF".equalsIgnoreCase(staleFailsafe);
    }

    @PostConstruct
    public void init() {
        migrateLegacyFileIfNeeded();
        log.info("Motor de reglas iniciado: {} reglas activas.", countEnabled());
    }

    /** Importa el antiguo rules.conf a la BD una única vez (si la tabla está vacía). */
    private void migrateLegacyFileIfNeeded() {
        if (ruleRepository.count() > 0) return;
        Path path = Path.of(rulesFile);
        if (!Files.exists(path)) return;
        try {
            List<RuleEntity> imported = LegacyRulesParser.parse(Files.readString(path));
            if (!imported.isEmpty()) {
                ruleRepository.saveAll(imported);
                Path backup = path.resolveSibling(path.getFileName() + ".migrated.bak");
                Files.move(path, backup);
                log.info("Importadas {} reglas desde {} a la BD. Copia en {}.",
                        imported.size(), rulesFile, backup);
            }
        } catch (Exception e) {
            log.warn("No se pudo importar el archivo de reglas {}: {}", rulesFile, e.getMessage());
        }
    }

    private long countEnabled() {
        return ruleRepository.findByEnabledTrueOrderByPriorityAscIdAsc().size();
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledEvaluation() {
        evaluateAll(false);
    }

    public void evaluateAll(boolean isReset) {
        for (DeviceSession session : deviceRegistry.all()) {
            try {
                evaluateDevice(session, isReset);
            } catch (Exception e) {
                log.error("Error evaluando reglas para {}: {}", session.getDeviceId(), e.getMessage());
            }
        }
    }

    public void evaluateDevice(DeviceSession session, boolean isReset) {
        String deviceId = session.getDeviceId();
        LocalTime now = LocalTime.now();
        DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();

        List<RuleEntity> rules = ruleRepository.findByEnabledTrueOrderByPriorityAscIdAsc()
                .stream()
                .filter(r -> deviceIdOf(r).equals(deviceId))
                .toList();

        boolean sensorStale = session.isSensorStale(sensorStalenessSeconds);
        if (sensorStale && hasSensorRule(rules)) {
            log.warn("Sensores de {} obsoletos (>{}s sin datos). Failsafe: {}.",
                    deviceId, sensorStalenessSeconds, staleFailsafeOff ? "OFF" : "KEEP");
        }

        Boolean[] desiredState = new Boolean[6];
        boolean[] governedBySensorRule = new boolean[6];

        for (RuleEntity rule : rules) {
            int relayIndex = rule.getRelayIndex();
            if (relayIndex < 0 || relayIndex >= 6) continue;
            if (session.isManualOverride(relayIndex)) continue;

            boolean usesSensor = rule.getTempMin() != null || rule.getTempMax() != null
                    || rule.getHumMin() != null || rule.getHumMax() != null;
            if (usesSensor) {
                governedBySensorRule[relayIndex] = true;
                if (sensorStale) continue; // sin datos fiables no se evalúan reglas de sensor
            }

            if (!matchesDay(rule, today)) continue;
            if (desiredState[relayIndex] != null) continue; // gana la regla de mayor prioridad

            // La histéresis solo ensancha umbrales cuando la acción mantiene el estado actual,
            // evitando el flapping alrededor del umbral.
            float hysteresis = widen(session, rule);
            if (rule.evaluate(now, session.getCurrentTemp(), session.getCurrentHum(), hysteresis)) {
                // Cooldown por regla: si la acción cambiaría el relé antes del intervalo
                // mínimo, se difiere este ciclo.
                boolean wouldChange = session.getRelay(relayIndex) != rule.getTargetState();
                if (!wouldChange || switchAllowed(session, relayIndex,
                        rule.getMinSwitchIntervalSeconds())) {
                    desiredState[relayIndex] = rule.getTargetState();
                }
            }
        }

        // Failsafe por sensor obsoleto
        if (sensorStale && staleFailsafeOff) {
            for (int i = 0; i < 6; i++) {
                if (governedBySensorRule[i] && !session.isManualOverride(i)) {
                    desiredState[i] = false;
                }
            }
        }

        applyDesiredStates(session, desiredState, isReset);
    }

    private static String deviceIdOf(RuleEntity rule) {
        return DeviceRegistry.normalize(rule.getDeviceId());
    }

    private boolean hasSensorRule(List<RuleEntity> rules) {
        return rules.stream().anyMatch(r -> r.getTempMin() != null || r.getTempMax() != null
                || r.getHumMin() != null || r.getHumMax() != null);
    }

    /** Devuelve la histéresis aplicable si la regla pretende mantener el estado actual del relé. */
    private float widen(DeviceSession session, RuleEntity rule) {
        float h = rule.getHysteresis() == null ? 0f : Math.max(0f, rule.getHysteresis());
        if (h == 0f) return 0f;
        return session.getRelay(rule.getRelayIndex()) == rule.getTargetState() ? h : 0f;
    }

    private void applyDesiredStates(DeviceSession session, Boolean[] desiredState, boolean isReset) {
        for (int i = 0; i < 6; i++) {
            if (session.isManualOverride(i)) continue;

            boolean target;
            if (desiredState[i] != null) {
                target = desiredState[i];
            } else if (isReset) {
                target = false;
            } else {
                continue; // sin regla aplicable: mantener estado actual
            }

            boolean current = session.getRelay(i);
            if (current == target) continue;

            session.setRelay(i, target);
            historyService.insertRelayEvent(session.getDeviceId(), i, target,
                    isReset ? "AUTOMATIC (RESET)" : "AUTOMATIC");
            log.info("Regla ejecutada: [{}] relé {} cambiado a {}",
                    session.getDeviceId(), i + 1, target ? "ON" : "OFF");
            deviceRegistry.fireRelayChanged(session.getDeviceId());
        }
    }

    /** El cooldown se respeta si no ha transcurrido el intervalo mínimo desde el último cambio. */
    private boolean switchAllowed(DeviceSession session, int relayIndex, int minIntervalSeconds) {
        if (minIntervalSeconds <= 0) return true;
        long elapsed = System.currentTimeMillis() - session.getLastSwitchAtMillis(relayIndex);
        return elapsed >= minIntervalSeconds * 1000L;
    }

    static boolean matchesDay(RuleEntity rule, DayOfWeek today) {
        String csv = rule.getDaysOfWeek();
        if (csv == null || csv.isBlank()) return true;
        Set<DayOfWeek> allowed = EnumSet.noneOf(DayOfWeek.class);
        Arrays.stream(csv.toUpperCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        allowed.add(DayOfWeek.valueOf(s));
                    } catch (IllegalArgumentException ignored) {}
                });
        return allowed.isEmpty() || allowed.contains(today);
    }
}

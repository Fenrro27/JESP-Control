package com.Fenrro.JESP_Core.service;

import com.Fenrro.JESP_Core.model.Rule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RulesEngine {

    private final DeviceState deviceState;
    private final HistoryService historyService;
    private final String rulesFile;

    private final List<Rule> rules = new ArrayList<>();

    public RulesEngine(DeviceState deviceState, HistoryService historyService,
                       @Value("${jesp.rules-file}") String rulesFile) {
        this.deviceState = deviceState;
        this.historyService = historyService;
        this.rulesFile = rulesFile;
    }

    @PostConstruct
    public void init() {
        loadRules();
    }

    public synchronized void loadRules() {
        rules.clear();
        Path path = Path.of(rulesFile);
        if (!Files.exists(path)) {
            crearReglasEjemplo(rulesFile);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile))) {
            String line;
            Rule currentRule = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.equals("---")) {
                    if (currentRule != null && currentRule.relayIndex != -1) {
                        rules.add(currentRule);
                    }
                    currentRule = null;
                    continue;
                }
                if (currentRule == null) currentRule = new Rule();

                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;
                String key = parts[0].trim().toUpperCase();
                String value = parts[1].trim();
                if (value.equals("-") || value.isEmpty()) continue;

                try {
                    switch (key) {
                        case "RELAY": currentRule.relayIndex = Integer.parseInt(value) - 1; break;
                        case "ACTION": currentRule.targetState = value.equalsIgnoreCase("ON"); break;
                        case "TIME_START": currentRule.timeStart = LocalTime.parse(value); break;
                        case "TIME_END": currentRule.timeEnd = LocalTime.parse(value); break;
                        case "TEMP_MIN": currentRule.tempMin = Float.parseFloat(value); break;
                        case "TEMP_MAX": currentRule.tempMax = Float.parseFloat(value); break;
                        case "HUM_MIN": currentRule.humMin = Float.parseFloat(value); break;
                        case "HUM_MAX": currentRule.humMax = Float.parseFloat(value); break;
                        case "CONDITION_LOGIC": currentRule.isAndLogic = value.equalsIgnoreCase("AND"); break;
                    }
                } catch (Exception e) {
                    log.error("Error parsing rule line: {}", line);
                }
            }
            if (currentRule != null && currentRule.relayIndex != -1) {
                rules.add(currentRule);
            }
            log.info("Cargadas {} reglas.", rules.size());
        } catch (Exception e) {
            log.warn("No se encontró o no se pudo cargar el archivo de reglas: {}. Usando modo manual.", rulesFile);
        }

        evaluateRules(true);
    }

    @Scheduled(fixedDelay = 5000)
    public synchronized void evaluateRules() {
        evaluateRules(false);
    }

    public synchronized void evaluateRules(boolean isReset) {
        LocalTime now = LocalTime.now();
        float temp = deviceState.getCurrentTemp();
        float hum = deviceState.getCurrentHum();

        Boolean[] desiredState = new Boolean[6];

        for (Rule rule : rules) {
            if (rule.relayIndex < 0 || rule.relayIndex >= 6) continue;
            if (deviceState.isManualOverride(rule.relayIndex)) continue;

            boolean conditionMet = rule.evaluate(temp, hum, now);
            if (conditionMet) {
                desiredState[rule.relayIndex] = rule.targetState;
            }
        }

        for (int i = 0; i < 6; i++) {
            if (deviceState.isManualOverride(i)) continue;

            boolean target;
            if (desiredState[i] != null) {
                target = desiredState[i];
            } else {
                if (isReset) {
                    target = false;
                } else {
                    target = deviceState.getRelay(i);
                }
            }

            if (deviceState.getRelay(i) != target) {
                deviceState.setRelay(i, target);
                String reason = isReset ? "AUTOMATIC (RESET)" : "AUTOMATIC";
                historyService.insertRelayEvent(i, target, reason);
                log.info("Regla ejecutada: Rele {} cambiado a {}", i + 1, target ? "ON" : "OFF");
            }
        }
    }

    private void crearReglasEjemplo(String filename) {
        try {
            Files.writeString(Path.of(filename),
                "# Archivo de configuración de reglas para JESP-Control\n" +
                "# Formato soportado por línea: CLAVE=VALOR\n" +
                "# Utiliza '---' para separar las reglas.\n" +
                "# CLAVES DISPONIBLES: RELAY (1-6), ACTION (ON/OFF), TIME_START (HH:mm), TIME_END (HH:mm), TEMP_MIN, TEMP_MAX, HUM_MIN, HUM_MAX, CONDITION_LOGIC (AND/OR)\n\n" +
                "# Ejemplo 1: Encender el relé 1 si la temperatura supera los 30 grados\n" +
                "RELAY=1\n" +
                "ACTION=ON\n" +
                "TEMP_MIN=30.0\n" +
                "CONDITION_LOGIC=AND\n" +
                "---\n\n" +
                "# Ejemplo 2: Encender el relé 2 todos los días de 18:00 a 22:00\n" +
                "RELAY=2\n" +
                "ACTION=ON\n" +
                "TIME_START=18:00\n" +
                "TIME_END=22:00\n" +
                "CONDITION_LOGIC=AND\n" +
                "---\n"
            );
            log.info("Archivo de reglas de ejemplo creado en: {}", filename);
        } catch (Exception e) {
            log.warn("No se pudo crear el archivo de reglas de ejemplo.");
        }
    }
}
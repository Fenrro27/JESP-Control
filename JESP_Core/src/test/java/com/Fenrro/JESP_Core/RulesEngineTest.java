package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import com.Fenrro.JESP_Core.repository.RuleRepository;
import com.Fenrro.JESP_Core.service.DeviceRegistry;
import com.Fenrro.JESP_Core.service.DeviceSession;
import com.Fenrro.JESP_Core.service.RulesEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "jesp.ws-port=0",
        "jesp.rules-file=target/test-rules-engine.conf",
        "spring.datasource.url=jdbc:sqlite:target/test-db-rules-engine.db"
})
@Transactional
class RulesEngineTest {

    @Autowired
    private RulesEngine rulesEngine;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private RuleRepository ruleRepository;

    private RuleEntity sensorRule(int relay, boolean on, Float tempMin) {
        RuleEntity rule = new RuleEntity();
        rule.setRelayIndex(relay);
        rule.setTargetState(on);
        rule.setEnabled(true);
        rule.setPriority(100);
        rule.setTempMin(tempMin);
        rule.setConditionLogic("AND");
        return rule;
    }

    @Test
    void temperatureRuleFiresAndHysteresisKeepsState() {
        DeviceSession session = deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID);
        session.resetOverrides();
        session.setRelaySilently(0, false);
        ruleRepository.save(sensorRule(0, true, 30.0f));

        session.markSeen(35.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(0), "Debe encender por encima de 30");

        // Bajamos de umbral: la histéresis por defecto (0.5) mantiene el estado ON
        session.markSeen(20.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(0), "Sin regla aplicable se mantiene el estado actual");

        rulesEngine.evaluateDevice(session, true); // reset apaga
        assertFalse(session.getRelay(0));
    }

    @Test
    void manualOverrideBlocksRule() {
        DeviceSession session = deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID);
        session.resetOverrides();
        session.setRelaySilently(0, false);
        ruleRepository.save(sensorRule(0, true, 30.0f));

        session.setOverride(0, true);
        session.markSeen(35.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertFalse(session.getRelay(0), "El override manual bloquea la regla");

        session.setOverride(0, false);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(0));
    }

    @Test
    void higherPriorityWinsForSameRelay() {
        DeviceSession session = deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID);
        session.resetOverrides();

        // Dos reglas simultáneas sobre el relé 1: la de prioridad menor (mayor prioridad) gana.
        // Ambas con condiciones siempre verdaderas en el instante de evaluación no es trivial,
        // así que usamos una ventana horaria amplia y TEMP_MIN muy bajo.
        RuleEntity high = sensorRule(0, false, null);
        high.setPriority(1);
        high.setTimeStart("00:00");
        high.setTimeEnd("23:59");
        ruleRepository.save(high);

        RuleEntity low = sensorRule(0, true, -999.0f);
        low.setPriority(50);
        ruleRepository.save(low);

        session.markSeen(25.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertFalse(session.getRelay(0), "La regla de mayor prioridad (OFF) debe ganar");

        // Desactivamos la de alta prioridad: ahora gana la otra
        high.setEnabled(false);
        ruleRepository.save(high);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(0));
    }

    @Test
    void staleSensorTriggersFailsafeOff() throws Exception {
        DeviceSession session = deviceRegistry.getOrCreate("stale-device");
        session.resetOverrides();

        RuleEntity rule = sensorRule(1, true, 10.0f);
        rule.setDeviceId("stale-device");
        ruleRepository.save(rule);

        // Dato fresco: enciende
        session.markSeen(25.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(1));

        // Simulamos obsolescencia retrocediendo lastSeen mediante reflexión no es posible;
        // en su lugar esperamos que con staleness configurado alto el test usa otro camino:
        // marcamos seen hace mucho tiempo usando un session con lastSeen antiguo.
        var field = DeviceSession.class.getDeclaredField("lastSeen");
        field.setAccessible(true);
        field.set(session, java.time.Instant.now().minusSeconds(3600));

        rulesEngine.evaluateDevice(session, false);
        assertFalse(session.getRelay(1), "Failsafe OFF debe apagar relés gobernados por sensores");
    }

    @Test
    void cooldownDefersRelaySwitching() throws Exception {
        DeviceSession session = deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID);
        session.resetOverrides();

        RuleEntity rule = sensorRule(2, true, 30.0f);
        rule.setMinSwitchIntervalSeconds(600); // 10 minutos de cooldown
        ruleRepository.save(rule);

        session.markSeen(35.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(2));

        // Reset + condición que exigiría apagar inmediatamente: el cooldown difiere el cambio
        rulesEngine.evaluateDevice(session, true);
        assertTrue(session.getRelay(2), "El reset ignora el cooldown");

        // Forzamos estado OFF silenciosamente y comprobamos que el cooldown impide reencender
        session.setRelaySilently(2, false);
        var field = DeviceSession.class.getDeclaredField("lastSwitchAtMillis");
        field.setAccessible(true);
        ((long[]) field.get(session))[2] = System.currentTimeMillis();
        rulesEngine.evaluateDevice(session, false);
        assertFalse(session.getRelay(2), "El cooldown difiere la nueva conmutación");

        // Expirado el intervalo, vuelve a actuar
        ((long[]) field.get(session))[2] = System.currentTimeMillis() - 601_000;
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(2));
    }

    @Test
    void daysOfWeekFilterApplies() {
        DeviceSession session = deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID);
        session.resetOverrides();

        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        java.time.DayOfWeek otherDay = today == java.time.DayOfWeek.MONDAY
                ? java.time.DayOfWeek.TUESDAY : java.time.DayOfWeek.MONDAY;

        // Regla con ventana horaria amplia pero restringida a un día que NO es hoy
        RuleEntity wrongDay = new RuleEntity();
        wrongDay.setRelayIndex(3);
        wrongDay.setTargetState(true);
        wrongDay.setEnabled(true);
        wrongDay.setTimeStart("00:00");
        wrongDay.setTimeEnd("23:59");
        wrongDay.setDaysOfWeek(otherDay.name());
        ruleRepository.save(wrongDay);

        session.markSeen(25.0f, 40.0f);
        rulesEngine.evaluateDevice(session, false);
        assertFalse(session.getRelay(3), "No debe disparar en un día no permitido");

        // Ahora limitada al día actual: debe disparar
        wrongDay.setDaysOfWeek(today.name());
        ruleRepository.save(wrongDay);
        rulesEngine.evaluateDevice(session, false);
        assertTrue(session.getRelay(3), "Debe disparar en su día permitido");
    }
}

package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de la lógica de evaluación de RuleEntity (condiciones, histéresis, OR/AND). */
class RuleEntityEvaluationTest {

    private RuleEntity rule() {
        RuleEntity rule = new RuleEntity();
        rule.setRelayIndex(0);
        rule.setTargetState(true);
        rule.setConditionLogic("AND");
        return rule;
    }

    @Test
    void tempMinFiresAboveThreshold() {
        RuleEntity r = rule();
        r.setTempMin(30.0f);
        assertTrue(r.evaluate(LocalTime.NOON, 31.0f, 40f, 0f));
        assertFalse(r.evaluate(LocalTime.NOON, 29.9f, 40f, 0f));
    }

    @Test
    void hysteresisWidensBoundsWhenKeepingState() {
        RuleEntity r = rule();
        r.setTempMin(30.0f);
        // Con histéresis 1.0 el umbral efectivo baja a 29.0
        assertFalse(r.evaluate(LocalTime.NOON, 29.5f, 40f, 0f));
        assertTrue(r.evaluate(LocalTime.NOON, 29.5f, 40f, 1.0f));
    }

    @Test
    void timeWindowNormalAndOvernight() {
        RuleEntity r = rule();
        r.setTimeStart("18:00");
        r.setTimeEnd("22:00");
        assertFalse(r.evaluate(LocalTime.of(17, 59), 25f, 40f, 0f));
        assertTrue(r.evaluate(LocalTime.of(20, 0), 25f, 40f, 0f));

        r.setTimeStart("22:00");
        r.setTimeEnd("06:00");
        assertTrue(r.evaluate(LocalTime.of(23, 30), 25f, 40f, 0f));
        assertTrue(r.evaluate(LocalTime.of(3, 0), 25f, 40f, 0f));
        assertFalse(r.evaluate(LocalTime.of(12, 0), 25f, 40f, 0f));
    }

    @Test
    void invalidTimeNeverFires() {
        RuleEntity r = rule();
        r.setTimeStart("sin-hora");
        r.setTimeEnd("22:00");
        assertFalse(r.evaluate(LocalTime.NOON, 25f, 40f, 0f));
    }

    @Test
    void orLogicFiresWithAnyCondition() {
        RuleEntity r = rule();
        r.setConditionLogic("OR");
        r.setTempMin(30.0f);
        r.setTimeStart("18:00");
        r.setTimeEnd("22:00");

        // Fuera de horario pero con temperatura alta -> dispara
        assertTrue(r.evaluate(LocalTime.of(12, 0), 35f, 40f, 0f));
        // En horario con temperatura baja -> dispara
        assertTrue(r.evaluate(LocalTime.of(19, 0), 10f, 40f, 0f));
        // Ni horario ni temperatura -> no dispara
        assertFalse(r.evaluate(LocalTime.of(12, 0), 10f, 40f, 0f));
    }

    @Test
    void andLogicRequiresAllConditions() {
        RuleEntity r = rule();
        r.setTempMin(30.0f);
        r.setHumMax(50.0f);
        assertTrue(r.evaluate(LocalTime.NOON, 35f, 45f, 0f));
        assertFalse(r.evaluate(LocalTime.NOON, 35f, 60f, 0f));
    }

    @Test
    void ruleWithoutConditionsNeverFires() {
        RuleEntity r = rule();
        assertFalse(r.evaluate(LocalTime.NOON, 25f, 40f, 0f));
    }
}

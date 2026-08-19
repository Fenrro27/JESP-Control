package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.model.Rule;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleTest {

    @Test
    void andLogicRequiresAllConditions() {
        Rule rule = new Rule();
        rule.tempMin = 30.0f;
        rule.timeStart = LocalTime.of(8, 0);
        rule.timeEnd = LocalTime.of(20, 0);
        rule.isAndLogic = true;

        assertFalse(rule.evaluate(35.0f, 40.0f, LocalTime.of(21, 0)));
        assertTrue(rule.evaluate(35.0f, 40.0f, LocalTime.of(10, 0)));
        assertFalse(rule.evaluate(25.0f, 40.0f, LocalTime.of(10, 0)));
    }

    @Test
    void orLogicSatisfiesAnyCondition() {
        Rule rule = new Rule();
        rule.tempMin = 30.0f;
        rule.timeStart = LocalTime.of(8, 0);
        rule.timeEnd = LocalTime.of(20, 0);
        rule.isAndLogic = false;

        assertTrue(rule.evaluate(35.0f, 40.0f, LocalTime.of(21, 0)));
        assertTrue(rule.evaluate(20.0f, 40.0f, LocalTime.of(10, 0)));
        assertFalse(rule.evaluate(20.0f, 40.0f, LocalTime.of(21, 0)));
    }

    @Test
    void midnightCrossingWindow() {
        Rule rule = new Rule();
        rule.timeStart = LocalTime.of(22, 0);
        rule.timeEnd = LocalTime.of(6, 0);
        rule.isAndLogic = true;

        assertTrue(rule.evaluate(20.0f, 40.0f, LocalTime.of(23, 0)));
        assertTrue(rule.evaluate(20.0f, 40.0f, LocalTime.of(5, 0)));
        assertFalse(rule.evaluate(20.0f, 40.0f, LocalTime.of(12, 0)));
    }

    @Test
    void noConditionsReturnsFalseForAndLogic() {
        Rule rule = new Rule();
        rule.isAndLogic = true;

        assertFalse(rule.evaluate(20.0f, 40.0f, LocalTime.now()));
    }

    @Test
    void humidityRange() {
        Rule rule = new Rule();
        rule.humMin = 20.0f;
        rule.humMax = 60.0f;
        rule.isAndLogic = true;

        assertTrue(rule.evaluate(20.0f, 40.0f, LocalTime.now()));
        assertFalse(rule.evaluate(20.0f, 10.0f, LocalTime.now()));
        assertFalse(rule.evaluate(20.0f, 80.0f, LocalTime.now()));
    }
}
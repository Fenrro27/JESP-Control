package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.service.DeviceState;
import com.Fenrro.JESP_Core.service.RulesEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"jesp.ws-port=0", "jesp.rules-file=target/test-rules.conf"})
@DirtiesContext
class RulesEngineTest {

    @Autowired
    private RulesEngine rulesEngine;

    @Autowired
    private DeviceState deviceState;

    @Test
    void temperatureRuleFiresAndHysteresisKeepsState() throws Exception {
        Files.writeString(Path.of("target/test-rules.conf"),
            "RELAY=1\nACTION=ON\nTEMP_MIN=30.0\nCONDITION_LOGIC=AND\n---\n");
        rulesEngine.loadRules();

        deviceState.setTempAndHum(35.0f, 40.0f);
        rulesEngine.evaluateRules(false);
        assertTrue(deviceState.getRelay(0));

        deviceState.setTempAndHum(20.0f, 40.0f);
        rulesEngine.evaluateRules(false);
        assertTrue(deviceState.getRelay(0));

        rulesEngine.evaluateRules(true);
        assertFalse(deviceState.getRelay(0));
    }

    @Test
    void manualOverrideBlocksRule() throws Exception {
        Files.writeString(Path.of("target/test-rules.conf"),
            "RELAY=1\nACTION=ON\nTEMP_MIN=30.0\nCONDITION_LOGIC=AND\n---\n");
        rulesEngine.loadRules();

        deviceState.setOverride(0, true);
        deviceState.setTempAndHum(35.0f, 40.0f);
        rulesEngine.evaluateRules(false);
        assertFalse(deviceState.getRelay(0));

        deviceState.setOverride(0, false);
        deviceState.setTempAndHum(35.0f, 40.0f);
        rulesEngine.evaluateRules(false);
        assertTrue(deviceState.getRelay(0));
    }
}
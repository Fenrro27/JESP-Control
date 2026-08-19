package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.service.DeviceState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"jesp.ws-port=0", "jesp.rules-file=target/test-rules.conf"})
@AutoConfigureMockMvc
@DirtiesContext
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceState deviceState;

    @Test
    void stateReturnsExactContract() throws Exception {
        deviceState.resetOverrides();
        deviceState.setTempAndHum(25.5f, 40.0f);
        deviceState.setRelay(0, true);

        mockMvc.perform(get("/api/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"connected\": false, \"temp\": 25.5, \"hum\": 40.0, \"relays\": [true, false, false, false, false, false], \"overrides\": [false, false, false, false, false, false]}"));
    }

    @Test
    void relayPostSetsStateAndOverride() throws Exception {
        mockMvc.perform(post("/api/relay").param("relay", "2").param("state", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));

        assertTrue(deviceState.getRelay(2));
        assertTrue(deviceState.isManualOverride(2));
    }

    @Test
    void relayInvalidIndexReturns400() throws Exception {
        mockMvc.perform(post("/api/relay").param("relay", "9").param("state", "true"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resetOverrideClearsAndEvaluates() throws Exception {
        deviceState.setOverride(0, true);
        deviceState.setRelay(0, true);

        mockMvc.perform(post("/api/reset_override"))
            .andExpect(status().isOk());

        assertFalse(deviceState.isManualOverride(0));
    }

    @Test
    void historyReturnsArray() throws Exception {
        mockMvc.perform(get("/api/history").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void rulesRoundTrip() throws Exception {
        String content = "RELAY=1\nACTION=ON\n---\n";

        mockMvc.perform(post("/api/rules").contentType(MediaType.TEXT_PLAIN).content(content))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rules"))
            .andExpect(status().isOk())
            .andExpect(content().string(content));
    }
}
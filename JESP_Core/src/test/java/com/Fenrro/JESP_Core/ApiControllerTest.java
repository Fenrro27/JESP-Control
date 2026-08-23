package com.Fenrro.JESP_Core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static com.Fenrro.JESP_Core.AuthTestUtils.loginAsAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jesp.ws-port=0",
        "jesp.rules-file=target/test-rules-api.conf",
        "spring.datasource.url=jdbc:sqlite:target/test-db-api.db",
        "jesp.admin-password=admin123"
})
@AutoConfigureMockMvc
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeAll
    void loginOnce() throws Exception {
        token = loginAsAdmin(mockMvc);
    }

    @Test
    void stateReturnsContract() throws Exception {
        mockMvc.perform(get("/api/state").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").isBoolean())
                .andExpect(jsonPath("$.temp").isNumber())
                .andExpect(jsonPath("$.hum").isNumber())
                .andExpect(jsonPath("$.relays.length()").value(6))
                .andExpect(jsonPath("$.overrides.length()").value(6));
    }

    @Test
    void relayPostSetsStateAndOverride() throws Exception {
        mockMvc.perform(post("/api/relay")
                        .param("relay", "2").param("state", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devices/esp32-default/state")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.relays[2]").value(true))
                .andExpect(jsonPath("$.overrides[2]").value(true));
    }

    @Test
    void relayInvalidIndexReturns400() throws Exception {
        mockMvc.perform(post("/api/relay")
                        .param("relay", "9").param("state", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetOverrideClearsFlag() throws Exception {
        mockMvc.perform(post("/api/relay")
                        .param("relay", "0").param("state", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reset_override").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devices/esp32-default/state")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.overrides[0]").value(false));
    }

    @Test
    void devicesEndpointListsDefaultDevice() throws Exception {
        mockMvc.perform(get("/api/devices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == 'esp32-default')]").exists());
    }

    @Test
    void historyReturnsArray() throws Exception {
        mockMvc.perform(get("/api/history").param("limit", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void historyFiltersByDateRange() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("from", "2000-01-01T00:00:00")
                        .param("to", "2099-01-01T00:00:00")
                        .param("limit", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void rulesCrudRoundTrip() throws Exception {
        String body = """
                {"name":"Ventilacion","deviceId":"esp32-default","relayIndex":1,
                 "targetState":true,"priority":10,"tempMin":30.0,
                 "conditionLogic":"AND","hysteresis":0.5,"minSwitchIntervalSeconds":60}
                """;

        // CREATE
        String created = mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // READ
        mockMvc.perform(get("/api/rules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // UPDATE (renombrar)
        String renamed = body.replace("Ventilacion", "Ventilacion v2");
        mockMvc.perform(put("/api/rules/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renamed)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ventilacion v2"));

        // DELETE
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/rules/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidRuleIsRejected() throws Exception {
        String invalid = """
                {"name":"Mala","relayIndex":9,"tempMin":30.0}
                """;
        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}

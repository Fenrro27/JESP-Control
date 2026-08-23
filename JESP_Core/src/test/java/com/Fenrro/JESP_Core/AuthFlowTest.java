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

import static com.Fenrro.JESP_Core.AuthTestUtils.ADMIN_PASSWORD;
import static com.Fenrro.JESP_Core.AuthTestUtils.ADMIN_USER;
import static com.Fenrro.JESP_Core.AuthTestUtils.login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jesp.ws-port=0",
        "jesp.rules-file=target/test-rules-auth.conf",
        "spring.datasource.url=jdbc:sqlite:target/test-db-auth.db",
        "jesp.admin-password=admin123"
})
@AutoConfigureMockMvc
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeAll
    void loginAdmin() throws Exception {
        adminToken = login(mockMvc, ADMIN_USER, ADMIN_PASSWORD);
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USER + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.username").value(ADMIN_USER));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/state"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleCanReadButNotWrite() throws Exception {
        // Creamos un usuario USER (si ya existe de una ejecución previa, se reutiliza)
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"consulta\",\"password\":\"consulta123\",\"role\":\"USER\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        String userToken = login(mockMvc, "consulta", "consulta123");

        // GET permitido
        mockMvc.perform(get("/api/state").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Escritura prohibida
        mockMvc.perform(post("/api/relay")
                        .param("relay", "0").param("state", "true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Gestión de usuarios prohibida
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListUsers() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.username == 'admin')]").exists());
    }
}

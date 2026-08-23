package com.Fenrro.JESP_Core;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Utilidades para obtener tokens JWT en pruebas de integración. */
final class AuthTestUtils {

    static final String ADMIN_USER = "admin";
    static final String ADMIN_PASSWORD = "admin123";

    private AuthTestUtils() {}

    static String login(MockMvc mockMvc, String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> map = new ObjectMapper().readValue(body, Map.class);
        return String.valueOf(map.get("token"));
    }

    static String loginAsAdmin(MockMvc mockMvc) throws Exception {
        return login(mockMvc, ADMIN_USER, ADMIN_PASSWORD);
    }
}

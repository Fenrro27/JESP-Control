package jesp.controller;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    // Map of token -> role ("ADMIN" or "USER")
    private static final ConcurrentHashMap<String, String> activeTokens = new ConcurrentHashMap<>();

    public static String createToken(String role) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, role);
        return token;
    }

    public static String getRole(String token) {
        if (token == null || token.isBlank()) return null;
        return activeTokens.get(token);
    }

    public static void revokeToken(String token) {
        if (token != null) {
            activeTokens.remove(token);
        }
    }
}

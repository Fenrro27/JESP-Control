package jesp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ApiLoginHandler extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String[] params = body.split("&");
        String username = "";
        String password = "";
        for (String p : params) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                if (kv[0].equals("username")) username = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                if (kv[0].equals("password")) password = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        
        String role = DatabaseManager.authenticateAndGetRole(username, password);
        if (role != null) {
            String token = AuthManager.createToken(role);
            resp.setContentType("application/json");
            resp.setStatus(200);
            resp.getWriter().write("{\"token\":\"" + token + "\", \"role\":\"" + role + "\"}");
        } else {
            resp.setStatus(401);
            resp.getWriter().write("Unauthorized");
        }
    }
}

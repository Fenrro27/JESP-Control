package jesp.controller;

import jesp.model.DeviceState;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

public class ApiRelayHandler extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = AuthManager.getRole(Esp32Server.extractToken(req));
        if (role == null) {
            resp.setStatus(401);
            return;
        }

        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String[] params = body.split("&");
        int relayIndex = -1;
        boolean state = false;
        
        for (String p : params) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                if (kv[0].equals("relay")) relayIndex = Integer.parseInt(kv[1]);
                if (kv[0].equals("state")) state = Boolean.parseBoolean(kv[1]);
            }
        }

        if (relayIndex >= 0 && relayIndex < 6) {
            boolean previousState = DeviceState.relays[relayIndex];
            DeviceState.setRelay(relayIndex, state);
            
            if ("ADMIN".equals(role)) {
                DeviceState.overrideExpiration[relayIndex] = Long.MAX_VALUE;
            } else {
                // USER: 2 hours expiration
                DeviceState.overrideExpiration[relayIndex] = System.currentTimeMillis() + (2 * 60 * 60 * 1000);
            }

            if (previousState != state) {
                DatabaseManager.insertRelayEvent(relayIndex, state, "MANUAL_" + role);
            }
            resp.setStatus(200);
            resp.getWriter().write("OK");
        } else {
            resp.setStatus(400);
        }
    }
}

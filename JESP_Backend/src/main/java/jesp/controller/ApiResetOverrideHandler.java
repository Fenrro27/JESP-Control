package jesp.controller;

import jesp.model.DeviceState;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ApiResetOverrideHandler extends HttpServlet {
    private RulesEngine engine;
    public ApiResetOverrideHandler(RulesEngine engine) { this.engine = engine; }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = AuthManager.getRole(Esp32Server.extractToken(req));
        if (role == null) {
            resp.setStatus(401);
            return;
        }

        for (int i = 0; i < 6; i++) {
            DeviceState.overrideExpiration[i] = 0;
        }
        
        engine.evaluateRules(true);
        resp.setStatus(200);
        resp.getWriter().write("OK");
    }
}

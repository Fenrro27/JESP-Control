package jesp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ApiRulesHandler extends HttpServlet {
    private String rulesFile;
    private RulesEngine engine;

    public ApiRulesHandler(String rulesFile, RulesEngine engine) {
        this.rulesFile = rulesFile;
        this.engine = engine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = AuthManager.getRole(Esp32Server.extractToken(req));
        if (!"ADMIN".equals(role)) {
            resp.setStatus(401);
            return;
        }

        String content = Files.readString(Path.of(rulesFile));
        resp.setContentType("text/plain");
        resp.setStatus(200);
        resp.getWriter().write(content);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = AuthManager.getRole(Esp32Server.extractToken(req));
        if (!"ADMIN".equals(role)) {
            resp.setStatus(401);
            return;
        }

        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Files.writeString(Path.of(rulesFile), body);
        engine.loadRules(rulesFile);
        resp.setStatus(200);
        resp.getWriter().write("OK");
    }
}

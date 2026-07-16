package jesp.controller;

import jesp.model.*;
import jesp.controller.*;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Esp32Server {
    private String rulesFile;
    private RulesEngine engine;

    public Esp32Server(String rulesFile, RulesEngine engine) {
        this.rulesFile = rulesFile;
        this.engine = engine;
    }

    public void start() throws Exception {
        Server server = new Server(5001);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // Path to static webapp files
        String webappDir = Esp32Server.class.getResource("/webapp") != null 
                           ? Esp32Server.class.getResource("/webapp").toExternalForm() 
                           : "src/main/resources/webapp";
                           
        context.setBaseResource(Resource.newResource(webappDir));
        server.setHandler(context);

        // API Endpoints
        context.addServlet(new ServletHolder(new ApiLoginHandler()), "/api/login");
        context.addServlet(new ServletHolder(new ApiStateHandler()), "/api/state");
        context.addServlet(new ServletHolder(new ApiHistoryHandler()), "/api/history");
        
        context.addServlet(new ServletHolder(new ApiRelayHandler()), "/api/relay");
        context.addServlet(new ServletHolder(new ApiResetOverrideHandler(engine)), "/api/reset_override");
        context.addServlet(new ServletHolder(new ApiRulesHandler(rulesFile, engine)), "/api/rules");

        // Static files fallback (Must be added last!)
        context.addServlet(DefaultServlet.class, "/");

        server.start();
        System.out.println("Servidor Jetty (Web + API) iniciado en el puerto 5001");
    }

    public static String extractToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

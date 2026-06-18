package jesp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Esp32Server {
    private String rulesFile;
    private RulesEngine engine;

    public Esp32Server(String rulesFile, RulesEngine engine) {
        this.rulesFile = rulesFile;
        this.engine = engine;
    }

    public void start() throws IOException {
        HttpServer serverApi = HttpServer.create(new InetSocketAddress(5001), 0);
        serverApi.createContext("/api/state", new ApiStateHandler());
        serverApi.createContext("/api/relay", new ApiRelayHandler());
        serverApi.createContext("/api/reset_override", new ApiResetOverrideHandler(engine));
        serverApi.createContext("/api/history", new ApiHistoryHandler());
        serverApi.createContext("/api/rules", new ApiRulesHandler(rulesFile, engine));
        serverApi.setExecutor(null);
        serverApi.start();
        System.out.println("Servidor API Desktop iniciado en el puerto 5001");
    }

    static class ApiStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"temp\": ").append(DeviceState.currentTemp)
              .append(", \"hum\": ").append(DeviceState.currentHum)
              .append(", \"relays\": [");
            for (int i = 0; i < 6; i++) {
                sb.append(DeviceState.relays[i]);
                if (i < 5) sb.append(", ");
            }
            sb.append("], \"overrides\": [");
            for (int i = 0; i < 6; i++) {
                sb.append(DeviceState.manualOverride[i]);
                if (i < 5) sb.append(", ");
            }
            sb.append("]}");

            String response = sb.toString();
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class ApiRelayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String[] params = body.split("&");
                int relayIndex = -1;
                boolean state = false;
                
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        if (kv[0].equals("relay")) relayIndex = Integer.parseInt(kv[1]);
                        if (kv[0].equals("state")) state = Boolean.parseBoolean(kv[1]);
                    }
                }

                if (relayIndex >= 0 && relayIndex < 6) {
                    boolean previousState = DeviceState.relays[relayIndex];
                    DeviceState.setRelay(relayIndex, state);
                    DeviceState.manualOverride[relayIndex] = true;
                    if (previousState != state) {
                        DatabaseManager.insertRelayEvent(relayIndex, state, "MANUAL");
                    }
                    String response = "OK";
                    t.sendResponseHeaders(200, response.length());
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    t.sendResponseHeaders(400, -1);
                }
            } else {
                t.sendResponseHeaders(405, -1);
            }
            t.close();
        }
    }

    static class ApiResetOverrideHandler implements HttpHandler {
        private RulesEngine engine;

        public ApiResetOverrideHandler(RulesEngine engine) {
            this.engine = engine;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                for (int i = 0; i < 6; i++) {
                    DeviceState.manualOverride[i] = false;
                }
                
                // Forzar reevaluación de reglas asumiendo un estado de reset (OFF para lo que no tenga regla)
                engine.evaluateRules(true);
                
                String response = "OK";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, -1);
                t.close();
            }
        }
    }

    static class ApiHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            int limit = 100; // default
            String query = t.getRequestURI().getQuery();
            if (query != null && query.contains("limit=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("limit=")) {
                        try {
                            limit = Integer.parseInt(param.split("=")[1]);
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            String response = DatabaseManager.getRecentSensorHistory(limit);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiRulesHandler implements HttpHandler {
        private String rulesFile;
        private RulesEngine engine;

        public ApiRulesHandler(String rulesFile, RulesEngine engine) {
            this.rulesFile = rulesFile;
            this.engine = engine;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equalsIgnoreCase(t.getRequestMethod())) {
                String content = java.nio.file.Files.readString(java.nio.file.Path.of(rulesFile));
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                t.getResponseHeaders().set("Content-Type", "text/plain");
                t.sendResponseHeaders(200, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            } else if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                java.nio.file.Files.writeString(java.nio.file.Path.of(rulesFile), body);
                engine.loadRules(rulesFile); // Reload rules
                String response = "OK";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, -1);
                t.close();
            }
        }
    }
}

package jesp_desktop.controller;

import jesp_desktop.model.ConfigManager;
import jesp_desktop.view.ControlPanelGUI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class BackendClient {
    private String baseUrl;
    private final HttpClient httpClient;
    
    public float temp = 0.0f;
    public float hum = 0.0f;
    public boolean[] relays = new boolean[6];
    public boolean[] overrides = new boolean[6];
    
    public boolean isConnected = false;
    public String token = null;
    public String role = "GUEST";
    public Runnable onConnectionChange;
    public Runnable onUpdate;

    public BackendClient() {
        this.baseUrl = "http://" + ConfigManager.getServerIp() + "/api";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public void setServerIp(String ip) {
        ConfigManager.setServerIp(ip);
        this.baseUrl = "http://" + ip + "/api";
    }

    public String getHistory() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/history?limit=100"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("HTTP status " + response.statusCode());
        }
    }

    public boolean login(String username, String password) {
        try {
            String body = "username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Parse simple json {"token":"...", "role":"..."} manually to avoid importing org.json in desktop client
                String[] parts = response.body().split("\"");
                for (int i=0; i<parts.length; i++) {
                    if (parts[i].equals("token")) token = parts[i+2];
                    if (parts[i].equals("role")) role = parts[i+2];
                }
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    public String getRules() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rules"))
                .GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpRequest request = builder.build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("HTTP status " + response.statusCode());
        }
    }

    public void saveRules(String rulesText) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rules"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(rulesText));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpRequest request = builder.build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
    }

    public void startPolling() {
        Thread t = new Thread(() -> {
            while (true) {
                boolean wasConnected = isConnected;
                try {
                    pollState();
                    isConnected = true;
                } catch (Exception e) {
                    isConnected = false;
                    System.err.println("No se pudo conectar al Backend: " + e.getMessage());
                }
                
                if (wasConnected != isConnected && onConnectionChange != null) {
                    onConnectionChange.run();
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void pollState() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/state"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            parseState(response.body());
            if (onUpdate != null) {
                onUpdate.run();
            }
        } else {
            throw new Exception("HTTP status " + response.statusCode());
        }
    }

    private void parseState(String json) {
        // Formato esperado: {"temp": 25.0, "hum": 40.0, "relays": [true, false, false, false, false, false], "overrides": [false, false, false, false, false, false]}
        try {
            String t = json.split("\"temp\": ")[1].split(",")[0].trim();
            this.temp = Float.parseFloat(t);
            
            String h = json.split("\"hum\": ")[1].split(",")[0].trim();
            this.hum = Float.parseFloat(h);
            
            String rStr = json.split("\"relays\": \\[")[1].split("\\]")[0];
            String[] rArr = rStr.split(",");
            for (int i = 0; i < 6; i++) {
                this.relays[i] = rArr[i].trim().equals("true");
            }

            String oStr = json.split("\"overrides\": \\[")[1].split("\\]")[0];
            String[] oArr = oStr.split(",");
            for (int i = 0; i < 6; i++) {
                this.overrides[i] = oArr[i].trim().equals("true");
            }
        } catch (Exception e) {
            System.err.println("Error parseando estado JSON del servidor: " + e.getMessage());
        }
    }

    public void setRelay(int index, boolean state) {
        try {
            String body = "relay=" + index + "&state=" + state;
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/relay"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (token != null) builder.header("Authorization", "Bearer " + token);
            HttpRequest request = builder.build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Error enviando comando setRelay al servidor: " + e.getMessage());
        }
    }

    public void resetOverrides() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/reset_override"))
                    .POST(HttpRequest.BodyPublishers.noBody());
            if (token != null) builder.header("Authorization", "Bearer " + token);
            HttpRequest request = builder.build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Error enviando comando resetOverrides al servidor: " + e.getMessage());
        }
    }
}

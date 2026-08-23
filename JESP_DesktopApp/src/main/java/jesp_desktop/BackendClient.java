package jesp_desktop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BackendClient {
    private String baseUrl;
    private final HttpClient httpClient;
    private String token;

    public float temp = 0.0f;
    public float hum = 0.0f;
    public boolean[] relays = new boolean[6];
    public boolean[] overrides = new boolean[6];

    public boolean isConnected = false;
    public boolean arduinoConnected = false;
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

    // ---------- Autenticación ----------

    /** Autentica contra el backend y guarda el token JWT en memoria. */
    public void login(String username, String password) throws Exception {
        JSONObject body = new JSONObject()
                .put("username", username)
                .put("password", password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new Exception("Usuario o contraseña incorrectos");
        }
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
        this.token = new JSONObject(response.body()).getString("token");
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    private HttpRequest.Builder authorized(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private void requireOk(HttpResponse<String> response) throws Exception {
        if (response.statusCode() == 401) {
            throw new Exception("Sesión expirada: vuelve a iniciar sesión");
        }
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode() + ": " + response.body());
        }
    }

    // ---------- Reglas (API JSON) ----------

    /** Devuelve la lista de reglas como JSONArray en texto. */
    public String getRulesJson() throws Exception {
        HttpRequest request = authorized(baseUrl + "/rules").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(response);
        return response.body();
    }

    public void createRule(JSONObject rule) throws Exception {
        HttpRequest request = authorized(baseUrl + "/rules")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rule.toString()))
                .build();
        requireOk(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    public void updateRule(long id, JSONObject rule) throws Exception {
        HttpRequest request = authorized(baseUrl + "/rules/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(rule.toString()))
                .build();
        requireOk(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    public void deleteRule(long id) throws Exception {
        HttpRequest request = authorized(baseUrl + "/rules/" + id)
                .DELETE()
                .build();
        requireOk(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
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
                    arduinoConnected = false;
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
        HttpRequest request = authorized(baseUrl + "/state")
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
        // Formato esperado: {"connected": true, "temp": 25.0, "hum": 40.0, "relays": [true, false, false, false, false, false], "overrides": [false, false, false, false, false, false]}
        try {
            try {
                String c = json.split("\"connected\": ")[1].split(",")[0].trim();
                this.arduinoConnected = c.equals("true");
            } catch (Exception ignored) {
                // campo ausente (backend antiguo) -> se conserva el estado anterior
            }

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
            HttpRequest request = authorized(baseUrl + "/relay")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Error enviando comando setRelay al servidor: " + e.getMessage());
        }
    }

    public void resetOverrides() {
        try {
            HttpRequest request = authorized(baseUrl + "/reset_override")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Error enviando comando resetOverrides al servidor: " + e.getMessage());
        }
    }

    public List<HistoryPoint> getHistoryChartData(int limit, String from, String to) throws Exception {
        HttpRequest request = authorized(baseUrl + "/history?limit=" + limit + "&from=" + from + "&to=" + to)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
        JSONArray arr = new JSONArray(response.body());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<HistoryPoint> points = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            HistoryPoint p = new HistoryPoint();
            LocalDateTime ts = LocalDateTime.parse(o.getString("timestamp"), formatter);
            p.timestampMillis = ts.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            p.temp = o.getDouble("temp");
            p.hum = o.getDouble("hum");
            points.add(p);
        }
        return points;
    }

    public List<HourStat> getHourlyStats(String from, String to) throws Exception {
        HttpRequest request = authorized(baseUrl + "/stats/hourly?from=" + from + "&to=" + to)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
        JSONArray arr = new JSONArray(response.body());
        List<HourStat> stats = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            HourStat s = new HourStat();
            s.hour = o.getInt("hour");
            s.avgTemp = o.isNull("avgTemp") ? null : o.getDouble("avgTemp");
            s.minTemp = o.isNull("minTemp") ? null : o.getDouble("minTemp");
            s.maxTemp = o.isNull("maxTemp") ? null : o.getDouble("maxTemp");
            s.avgHum = o.isNull("avgHum") ? null : o.getDouble("avgHum");
            s.records = o.getLong("records");
            stats.add(s);
        }
        return stats;
    }

    public Summary getSummary(String from, String to) throws Exception {
        HttpRequest request = authorized(baseUrl + "/stats/summary?from=" + from + "&to=" + to)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
        JSONObject o = new JSONObject(response.body());
        Summary s = new Summary();
        s.avgTemp = o.isNull("avgTemp") ? null : o.getDouble("avgTemp");
        s.minTemp = o.isNull("minTemp") ? null : o.getDouble("minTemp");
        s.maxTemp = o.isNull("maxTemp") ? null : o.getDouble("maxTemp");
        s.avgHum = o.isNull("avgHum") ? null : o.getDouble("avgHum");
        s.records = o.getLong("records");
        return s;
    }

    public Trend getTrend(String from, String to) throws Exception {
        HttpRequest request = authorized(baseUrl + "/stats/trend?from=" + from + "&to=" + to)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("HTTP status " + response.statusCode());
        }
        JSONObject o = new JSONObject(response.body());
        Trend t = new Trend();
        t.slopePerHour = o.getDouble("slopePerHour");
        t.delta = o.getDouble("delta");
        t.direction = o.getString("direction");
        t.samples = o.getLong("samples");
        return t;
    }

    public static class HistoryPoint {
        public long timestampMillis;
        public double temp;
        public double hum;
    }

    public static class HourStat {
        public int hour;
        public Double avgTemp;
        public Double minTemp;
        public Double maxTemp;
        public Double avgHum;
        public long records;
    }

    public static class Summary {
        public Double avgTemp;
        public Double minTemp;
        public Double maxTemp;
        public Double avgHum;
        public long records;

        public boolean isEmpty() {
            return records == 0 || avgTemp == null;
        }
    }

    public static class Trend {
        public double slopePerHour;
        public double delta;
        public String direction;
        public long samples;
    }
}

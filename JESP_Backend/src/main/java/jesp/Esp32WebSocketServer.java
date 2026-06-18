package jesp;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Esp32WebSocketServer extends WebSocketServer {
    
    private final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());

    public Esp32WebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nuevo ESP32 conectado vía WebSocket: " + conn.getRemoteSocketAddress());
        connections.add(conn);
        sendCurrentConfig(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("ESP32 desconectado: " + conn.getRemoteSocketAddress());
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Expected message: {"temp": 25.5, "hum": 40.0}
        try {
            JSONObject json = new JSONObject(message);
            if (json.has("temp") && json.has("hum")) {
                float temp = (float) json.getDouble("temp");
                float hum = (float) json.getDouble("hum");
                DeviceState.setTempAndHum(temp, hum);
                DatabaseManager.insertSensorData(temp, hum);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje WS: " + message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error en WebSocket: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket ESP32 iniciado en el puerto " + getPort());
    }

    public void broadcastCurrentConfig() {
        JSONObject config = new JSONObject();
        boolean[] relays = DeviceState.relays;
        config.put("reles", relays);
        String msg = config.toString();
        for (WebSocket conn : connections) {
            conn.send(msg);
        }
    }

    private void sendCurrentConfig(WebSocket conn) {
        JSONObject config = new JSONObject();
        boolean[] relays = DeviceState.relays;
        config.put("reles", relays);
        conn.send(config.toString());
    }
}

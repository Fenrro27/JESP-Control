package com.Fenrro.JESP_Core.service;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor WebSocket para dispositivos físicos (ESP32).
 * Cada conexión puede identificarse enviando {"id": "mi-dispositivo"} en cualquier
 * mensaje; las conexiones sin identificador se asignan a {@link DeviceRegistry#DEFAULT_DEVICE_ID},
 * manteniendo la compatibilidad con el firmware actual.
 */
@Slf4j
@Component
public class Esp32WebSocketServer extends WebSocketServer {

    private final DeviceRegistry deviceRegistry;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Mapa conexión -> deviceId normalizado. */
    private final Map<WebSocket, String> connections = new ConcurrentHashMap<>();

    public Esp32WebSocketServer(DeviceRegistry deviceRegistry,
                                HistoryService historyService,
                                @Value("${jesp.ws-port}") int port) {
        super(new InetSocketAddress(port));
        this.deviceRegistry = deviceRegistry;
        this.historyService = historyService;
    }

    @PostConstruct
    public void init() {
        deviceRegistry.onRelayChanged(this::broadcastCurrentConfig);
        try {
            start();
            log.info("Servidor WebSocket ESP32 iniciado en el puerto {}", getPort());
        } catch (Exception e) {
            log.error("Error iniciando servidor WebSocket: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            stop(1000);
        } catch (Exception e) {
            log.warn("Error deteniendo servidor WebSocket: {}", e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.put(conn, DeviceRegistry.DEFAULT_DEVICE_ID);
        log.info("Nuevo dispositivo conectado vía WebSocket: {}", conn.getRemoteSocketAddress());
        sendCurrentConfig(conn, DeviceRegistry.DEFAULT_DEVICE_ID);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String deviceId = connections.remove(conn);
        log.info("Dispositivo desconectado ({}): {}", deviceId, conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> json = objectMapper.readValue(message, Map.class);

            // Identificación del dispositivo (compatible hacia atrás: sin "id" => por defecto)
            if (json.containsKey("id")) {
                try {
                    String deviceId = DeviceRegistry.normalize(String.valueOf(json.get("id")));
                    connections.put(conn, deviceId);
                } catch (IllegalArgumentException e) {
                    log.warn("deviceId inválido descartado: {}", e.getMessage());
                }
            }
            String deviceId = connections.getOrDefault(conn, DeviceRegistry.DEFAULT_DEVICE_ID);

            if (json.containsKey("temp") && json.containsKey("hum")) {
                float temp = ((Number) json.get("temp")).floatValue();
                float hum = ((Number) json.get("hum")).floatValue();
                DeviceSession session = deviceRegistry.getOrCreate(deviceId);
                session.markSeen(temp, hum);
                historyService.insertSensorData(deviceId, temp, hum);
            }
        } catch (Exception e) {
            log.error("Error procesando mensaje WS: {}", message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("Error en WebSocket: {}", ex.getMessage());
    }

    @Override
    public void onStart() {
        log.info("Servidor WebSocket ESP32 listo en el puerto {}", getPort());
    }

    public boolean isDeviceConnected() {
        return isDeviceConnected(DeviceRegistry.DEFAULT_DEVICE_ID);
    }

    public boolean isDeviceConnected(String deviceId) {
        return connections.containsValue(deviceId);
    }

    public void broadcastCurrentConfig(String deviceId) {
        try {
            DeviceSession session = deviceRegistry.getOrCreate(deviceId);
            String msg = objectMapper.writeValueAsString(Map.of("reles", session.getRelays()));
            for (Map.Entry<WebSocket, String> entry : connections.entrySet()) {
                if (entry.getValue().equals(deviceId)) {
                    entry.getKey().send(msg);
                }
            }
        } catch (Exception e) {
            log.error("Error transmitiendo configuración de relés de {}: {}",
                    deviceId, e.getMessage());
        }
    }

    private void sendCurrentConfig(WebSocket conn, String deviceId) {
        try {
            DeviceSession session = deviceRegistry.getOrCreate(deviceId);
            String msg = objectMapper.writeValueAsString(Map.of("reles", session.getRelays()));
            conn.send(msg);
        } catch (Exception e) {
            log.error("Error enviando configuración inicial: {}", e.getMessage());
        }
    }
}

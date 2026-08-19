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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class Esp32WebSocketServer extends WebSocketServer {

    private final DeviceState deviceState;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());

    public Esp32WebSocketServer(DeviceState deviceState, HistoryService historyService,
                                @Value("${jesp.ws-port}") int port) {
        super(new InetSocketAddress(port));
        this.deviceState = deviceState;
        this.historyService = historyService;
    }

    @PostConstruct
    public void init() {
        deviceState.setOnRelayChanged(this::broadcastCurrentConfig);
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
        log.info("Nuevo ESP32 conectado vía WebSocket: {}", conn.getRemoteSocketAddress());
        connections.add(conn);
        sendCurrentConfig(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("ESP32 desconectado: {}", conn.getRemoteSocketAddress());
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> json = objectMapper.readValue(message, Map.class);
            if (json.containsKey("temp") && json.containsKey("hum")) {
                float temp = ((Number) json.get("temp")).floatValue();
                float hum = ((Number) json.get("hum")).floatValue();
                deviceState.setTempAndHum(temp, hum);
                historyService.insertSensorData(temp, hum);
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
        return !connections.isEmpty();
    }

    public void broadcastCurrentConfig() {
        try {
            String msg = objectMapper.writeValueAsString(Map.of("reles", deviceState.getRelays()));
            for (WebSocket conn : connections) {
                conn.send(msg);
            }
        } catch (Exception e) {
            log.error("Error transmitiendo configuración de relés: {}", e.getMessage());
        }
    }

    private void sendCurrentConfig(WebSocket conn) {
        try {
            String msg = objectMapper.writeValueAsString(Map.of("reles", deviceState.getRelays()));
            conn.send(msg);
        } catch (Exception e) {
            log.error("Error enviando configuración inicial: {}", e.getMessage());
        }
    }
}
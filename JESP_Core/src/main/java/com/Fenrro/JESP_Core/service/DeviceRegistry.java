package com.Fenrro.JESP_Core.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registro central de dispositivos conectados. Sustituye al antiguo DeviceState singleton:
 * ahora cada dispositivo (identificado por su deviceId) tiene su propio DeviceSession.
 * Los dispositivos que no se identifican se asignan a {@link #DEFAULT_DEVICE_ID},
 * lo que mantiene la compatibilidad con el firmware actual.
 */
@Component
public class DeviceRegistry {

    public static final String DEFAULT_DEVICE_ID = "esp32-default";

    private final Map<String, DeviceSession> sessions = Collections.synchronizedMap(
            new LinkedHashMap<>());

    private final List<Consumer<String>> relayChangeListeners = new ArrayList<>();

    public synchronized DeviceSession getOrCreate(String deviceId) {
        String id = normalize(deviceId);
        return sessions.computeIfAbsent(id, DeviceSession::new);
    }

    public Collection<DeviceSession> all() {
        synchronized (sessions) {
            return List.copyOf(sessions.values());
        }
    }

    public boolean exists(String deviceId) {
        return sessions.containsKey(normalize(deviceId));
    }

    public void onRelayChanged(Consumer<String> listener) {
        relayChangeListeners.add(listener);
    }

    public void fireRelayChanged(String deviceId) {
        for (Consumer<String> listener : relayChangeListeners) {
            try {
                listener.accept(normalize(deviceId));
            } catch (Exception ignored) {
                // un oyente defectuoso no debe romper el flujo
            }
        }
    }

    public static String normalize(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return DEFAULT_DEVICE_ID;
        }
        String cleaned = deviceId.trim();
        if (!cleaned.matches("[A-Za-z0-9_\\-.]{1,64}")) {
            throw new IllegalArgumentException("deviceId inválido: " + deviceId);
        }
        return cleaned;
    }
}

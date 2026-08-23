package com.Fenrro.JESP_Core.service;

import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;

/**
 * Estado en tiempo de ejecución de un dispositivo físico (p. ej. un módulo ESP32 de 6 relés).
 * Cada dispositivo conectado tiene su propia sesión con sus sensores y relés.
 */
public class DeviceSession {

    @Getter
    private final String deviceId;

    private float currentTemp = 0.0f;
    private float currentHum = 0.0f;
    private final boolean[] relays = new boolean[6];
    private final boolean[] manualOverride = new boolean[6];
    private final long[] lastSwitchAtMillis = new long[6];

    @Getter
    private volatile Instant lastSeen = null;

    DeviceSession(String deviceId) {
        this.deviceId = deviceId;
    }

    public synchronized float getCurrentTemp() {
        return currentTemp;
    }

    public synchronized float getCurrentHum() {
        return currentHum;
    }

    public synchronized boolean getRelay(int index) {
        return relays[index];
    }

    public synchronized boolean isManualOverride(int index) {
        return manualOverride[index];
    }

    public synchronized boolean[] getRelays() {
        return relays.clone();
    }

    public synchronized boolean[] getOverrides() {
        return manualOverride.clone();
    }

    public synchronized long getLastSwitchAtMillis(int index) {
        return lastSwitchAtMillis[index];
    }

    /** Marca de tiempo del último dato recibido del dispositivo. */
    public synchronized void markSeen(float temp, float hum) {
        currentTemp = temp;
        currentHum = hum;
        lastSeen = Instant.now();
    }

    public synchronized void setRelay(int index, boolean state) {
        if (relays[index] != state) {
            relays[index] = state;
            lastSwitchAtMillis[index] = System.currentTimeMillis();
        }
    }

    /** Cambia el relé sin registrar marca de conmutación (usado por failsafe/arranque). */
    public synchronized void setRelaySilently(int index, boolean state) {
        relays[index] = state;
    }

    public synchronized void setOverride(int index, boolean override) {
        manualOverride[index] = override;
    }

    public synchronized void resetOverrides() {
        Arrays.fill(manualOverride, false);
    }

    public synchronized boolean isSensorStale(long stalenessSeconds) {
        return lastSeen == null
                || lastSeen.isBefore(Instant.now().minusSeconds(stalenessSeconds));
    }

    @Override
    public String toString() {
        return "DeviceSession{" + deviceId + "}";
    }
}

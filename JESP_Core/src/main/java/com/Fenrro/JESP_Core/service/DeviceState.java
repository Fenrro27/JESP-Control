package com.Fenrro.JESP_Core.service;

import org.springframework.stereotype.Component;

@Component
public class DeviceState {

    private float currentTemp = 0.0f;
    private float currentHum = 0.0f;
    private final boolean[] relays = new boolean[6];
    private final boolean[] manualOverride = new boolean[6];

    private Runnable onStateChanged = null;
    private Runnable onRelayChanged = null;

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

    public void setOnStateChanged(Runnable runnable) {
        this.onStateChanged = runnable;
    }

    public void setOnRelayChanged(Runnable runnable) {
        this.onRelayChanged = runnable;
    }

    public synchronized void setTempAndHum(float temp, float hum) {
        currentTemp = temp;
        currentHum = hum;
        if (onStateChanged != null) onStateChanged.run();
    }

    public synchronized void setRelay(int index, boolean state) {
        relays[index] = state;
        if (onStateChanged != null) onStateChanged.run();
        if (onRelayChanged != null) onRelayChanged.run();
    }

    public synchronized void setOverride(int index, boolean override) {
        manualOverride[index] = override;
    }

    public synchronized void resetOverrides() {
        for (int i = 0; i < 6; i++) {
            manualOverride[i] = false;
        }
    }
}
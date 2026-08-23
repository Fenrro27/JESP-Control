package com.Fenrro.JESP_Core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica común de control manual de relés compartida por los endpoints
 * legacy (/api/relay) y por dispositivo (/api/devices/{id}/relay).
 */
@Service
@RequiredArgsConstructor
public class RelayControlService {

    private final DeviceRegistry deviceRegistry;
    private final HistoryService historyService;
    private final RulesEngine rulesEngine;

    public DeviceSession sessionOr404(String deviceId) {
        try {
            return deviceRegistry.getOrCreate(deviceId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public void setRelay(String deviceId, int relayIndex, boolean state) {
        DeviceSession session = sessionOr404(deviceId);
        if (relayIndex < 0 || relayIndex >= 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relay debe estar entre 0 y 5");
        }
        boolean previousState = session.getRelay(relayIndex);
        session.setRelay(relayIndex, state);
        session.setOverride(relayIndex, true);
        if (previousState != state) {
            historyService.insertRelayEvent(session.getDeviceId(), relayIndex, state, "MANUAL");
            deviceRegistry.fireRelayChanged(session.getDeviceId());
        }
    }

    public void resetOverrides(String deviceId) {
        DeviceSession session = sessionOr404(deviceId);
        session.resetOverrides();
        rulesEngine.evaluateDevice(session, true);
    }
}

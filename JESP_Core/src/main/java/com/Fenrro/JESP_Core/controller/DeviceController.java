package com.Fenrro.JESP_Core.controller;

import com.Fenrro.JESP_Core.service.DeviceRegistry;
import com.Fenrro.JESP_Core.service.DeviceSession;
import com.Fenrro.JESP_Core.service.Esp32WebSocketServer;
import com.Fenrro.JESP_Core.service.RelayControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * API de dispositivos. Los endpoints /api/state, /api/relay y /api/reset_override
 * se mantienen operando sobre el dispositivo por defecto (compatibilidad con
 * clientes legacy como la versión previa de JESP_DesktopApp).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceRegistry deviceRegistry;
    private final Esp32WebSocketServer esp32Server;
    private final RelayControlService relayControl;

    public record DeviceStateDto(boolean connected, double temp, double hum,
                                 boolean[] relays, boolean[] overrides) {}

    public record DeviceInfoDto(String id, boolean online, String lastSeen,
                                double temp, double hum,
                                boolean[] relays, boolean[] overrides) {}

    @GetMapping("/devices")
    public List<DeviceInfoDto> listDevices() {
        return deviceRegistry.all().stream().map(this::toInfo).toList();
    }

    @GetMapping("/devices/{deviceId}/state")
    public DeviceStateDto deviceState(@PathVariable String deviceId) {
        return toState(relayControl.sessionOr404(deviceId), esp32Server.isDeviceConnected(deviceId));
    }

    @PostMapping("/devices/{deviceId}/relay")
    public String setDeviceRelay(@PathVariable String deviceId,
                                 @RequestParam int relay, @RequestParam boolean state) {
        relayControl.setRelay(deviceId, relay, state);
        return "OK";
    }

    @PostMapping("/devices/{deviceId}/reset_override")
    public String resetDeviceOverride(@PathVariable String deviceId) {
        relayControl.resetOverrides(deviceId);
        return "OK";
    }

    // ---------- Endpoints legacy (dispositivo por defecto) ----------

    @GetMapping("/state")
    public DeviceStateDto state() {
        DeviceSession session = relayControl.sessionOr404(DeviceRegistry.DEFAULT_DEVICE_ID);
        return toState(session, esp32Server.isDeviceConnected());
    }

    @PostMapping("/relay")
    public String setRelay(@RequestParam int relay, @RequestParam boolean state) {
        relayControl.setRelay(DeviceRegistry.DEFAULT_DEVICE_ID, relay, state);
        return "OK";
    }

    @PostMapping("/reset_override")
    public String resetOverride() {
        relayControl.resetOverrides(DeviceRegistry.DEFAULT_DEVICE_ID);
        return "OK";
    }

    // ---------- helpers ----------

    private DeviceStateDto toState(DeviceSession session, boolean connected) {
        return new DeviceStateDto(connected, session.getCurrentTemp(), session.getCurrentHum(),
                session.getRelays(), session.getOverrides());
    }

    private DeviceInfoDto toInfo(DeviceSession session) {
        String lastSeen = session.getLastSeen() == null
                ? null
                : session.getLastSeen().atZone(ZoneId.systemDefault()).format(TS);
        return new DeviceInfoDto(session.getDeviceId(),
                esp32Server.isDeviceConnected(session.getDeviceId()),
                lastSeen,
                session.getCurrentTemp(), session.getCurrentHum(),
                session.getRelays(), session.getOverrides());
    }
}

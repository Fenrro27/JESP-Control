package com.Fenrro.JESP_Core.controller;

import com.Fenrro.JESP_Core.entity.SensorHistory;
import com.Fenrro.JESP_Core.repository.SensorHistoryRepository;
import com.Fenrro.JESP_Core.service.DeviceState;
import com.Fenrro.JESP_Core.service.Esp32WebSocketServer;
import com.Fenrro.JESP_Core.service.HistoryService;
import com.Fenrro.JESP_Core.service.RulesEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private static final DateTimeFormatter DB_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceState deviceState;
    private final HistoryService historyService;
    private final RulesEngine rulesEngine;
    private final Esp32WebSocketServer esp32Server;

    @Value("${jesp.rules-file}")
    private String rulesFile;

    @GetMapping(value = "/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public String state() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"connected\": ").append(esp32Server.isDeviceConnected())
          .append(", \"temp\": ").append(deviceState.getCurrentTemp())
          .append(", \"hum\": ").append(deviceState.getCurrentHum())
          .append(", \"relays\": [");
        boolean[] relays = deviceState.getRelays();
        for (int i = 0; i < 6; i++) {
            sb.append(relays[i]);
            if (i < 5) sb.append(", ");
        }
        sb.append("], \"overrides\": [");
        boolean[] overrides = deviceState.getOverrides();
        for (int i = 0; i < 6; i++) {
            sb.append(overrides[i]);
            if (i < 5) sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }

    @PostMapping("/relay")
    public ResponseEntity<String> setRelay(@RequestParam int relay, @RequestParam boolean state) {
        if (relay < 0 || relay >= 6) {
            return ResponseEntity.badRequest().build();
        }
        boolean previousState = deviceState.getRelay(relay);
        deviceState.setRelay(relay, state);
        deviceState.setOverride(relay, true);
        if (previousState != state) {
            historyService.insertRelayEvent(relay, state, "MANUAL");
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/reset_override")
    public ResponseEntity<String> resetOverride() {
        deviceState.resetOverrides();
        rulesEngine.evaluateRules(true);
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public String history(@RequestParam(defaultValue = "100") int limit,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String to) {
        List<SensorHistory> rows;
        if (from != null && to != null) {
            rows = historyService.getSensorHistoryBetween(LocalDateTime.parse(from), LocalDateTime.parse(to), limit);
        } else {
            rows = historyService.getRecentSensorHistory(limit);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(",");
            SensorHistory row = rows.get(i);
            sb.append("{\"timestamp\":\"").append(row.getTimestamp().format(DB_TIMESTAMP)).append("\",")
              .append("\"temp\":").append(row.getTemperature()).append(",")
              .append("\"hum\":").append(row.getHumidity()).append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    @GetMapping(value = "/stats/hourly", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hourlyStats(@RequestParam String from, @RequestParam String to) {
        List<SensorHistoryRepository.HourlyStat> rows =
                historyService.getHourlyProfile(LocalDateTime.parse(from), LocalDateTime.parse(to));

        Map<Integer, SensorHistoryRepository.HourlyStat> byHour = new HashMap<>();
        for (SensorHistoryRepository.HourlyStat row : rows) {
            byHour.put(row.getHour(), row);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int hour = 0; hour < 24; hour++) {
            if (hour > 0) sb.append(",");
            SensorHistoryRepository.HourlyStat row = byHour.get(hour);
            sb.append("{\"hour\":").append(hour)
              .append(",\"avgTemp\":").append(num(row == null ? null : row.getAvgTemp()))
              .append(",\"minTemp\":").append(num(row == null ? null : row.getMinTemp()))
              .append(",\"maxTemp\":").append(num(row == null ? null : row.getMaxTemp()))
              .append(",\"avgHum\":").append(num(row == null ? null : row.getAvgHum()))
              .append(",\"records\":").append(row == null ? 0 : row.getRecords())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    @GetMapping(value = "/stats/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public String summaryStats(@RequestParam String from, @RequestParam String to) {
        SensorHistoryRepository.SummaryStat row =
                historyService.getSummary(LocalDateTime.parse(from), LocalDateTime.parse(to));

        return "{\"avgTemp\":" + num(row.getAvgTemp())
             + ",\"minTemp\":" + num(row.getMinTemp())
             + ",\"maxTemp\":" + num(row.getMaxTemp())
             + ",\"avgHum\":" + num(row.getAvgHum())
             + ",\"records\":" + row.getRecords() + "}";
    }

    @GetMapping(value = "/stats/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    public String trendStats(@RequestParam String from, @RequestParam String to) {
        HistoryService.TrendResult trend = historyService.getTrend(LocalDateTime.parse(from), LocalDateTime.parse(to));
        return "{\"slopePerHour\":" + trend.slopePerHour()
             + ",\"delta\":" + trend.delta()
             + ",\"direction\":\"" + trend.direction()
             + "\",\"samples\":" + trend.samples() + "}";
    }

    private static String num(Double value) {
        return value == null ? "null" : value.toString();
    }

    @GetMapping(value = "/rules", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRules() {
        try {
            return ResponseEntity.ok(Files.readString(Path.of(rulesFile)));
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(value = "/rules", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> saveRules(@RequestBody String content) {
        try {
            Files.writeString(Path.of(rulesFile), content);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
        rulesEngine.loadRules();
        return ResponseEntity.ok("OK");
    }
}
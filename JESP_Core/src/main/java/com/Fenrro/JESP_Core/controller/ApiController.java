package com.Fenrro.JESP_Core.controller;

import com.Fenrro.JESP_Core.entity.SensorHistory;
import com.Fenrro.JESP_Core.repository.SensorHistoryRepository;
import com.Fenrro.JESP_Core.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private static final DateTimeFormatter DB_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HistoryService historyService;

    public record SensorPointDto(String timestamp, double temp, double hum) {}

    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SensorPointDto> history(@RequestParam(defaultValue = "100") int limit,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(name = "device", required = false) String device) {
        List<SensorHistory> rows;
        if (from != null && to != null) {
            rows = historyService.getSensorHistoryBetween(LocalDateTime.parse(from),
                    LocalDateTime.parse(to), device, limit);
        } else {
            rows = historyService.getRecentSensorHistory(device, limit);
        }
        return rows.stream()
                .map(r -> new SensorPointDto(r.getTimestamp().format(DB_TIMESTAMP),
                        r.getTemperature(), r.getHumidity()))
                .toList();
    }

    @GetMapping("/stats/hourly")
    public List<Map<String, Object>> hourlyStats(@RequestParam String from,
                                                 @RequestParam String to,
                                                 @RequestParam(name = "device", required = false) String device) {
        List<SensorHistoryRepository.HourlyStat> rows =
                historyService.getHourlyProfile(LocalDateTime.parse(from),
                        LocalDateTime.parse(to), device);

        Map<Integer, SensorHistoryRepository.HourlyStat> byHour = new HashMap<>();
        for (SensorHistoryRepository.HourlyStat row : rows) {
            byHour.put(row.getHour(), row);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            SensorHistoryRepository.HourlyStat row = byHour.get(hour);
            Map<String, Object> entry = new HashMap<>();
            entry.put("hour", hour);
            entry.put("avgTemp", row == null ? null : row.getAvgTemp());
            entry.put("minTemp", row == null ? null : row.getMinTemp());
            entry.put("maxTemp", row == null ? null : row.getMaxTemp());
            entry.put("avgHum", row == null ? null : row.getAvgHum());
            entry.put("records", row == null ? 0 : row.getRecords());
            result.add(entry);
        }
        return result;
    }

    @GetMapping("/stats/summary")
    public Map<String, Object> summaryStats(@RequestParam String from,
                                            @RequestParam String to,
                                            @RequestParam(name = "device", required = false) String device) {
        SensorHistoryRepository.SummaryStat row =
                historyService.getSummary(LocalDateTime.parse(from), LocalDateTime.parse(to), device);
        Map<String, Object> result = new HashMap<>();
        result.put("avgTemp", row.getAvgTemp());
        result.put("minTemp", row.getMinTemp());
        result.put("maxTemp", row.getMaxTemp());
        result.put("avgHum", row.getAvgHum());
        result.put("records", row.getRecords());
        return result;
    }

    @GetMapping("/stats/trend")
    public HistoryService.TrendResult trendStats(@RequestParam String from,
                                                 @RequestParam String to,
                                                 @RequestParam(name = "device", required = false) String device) {
        return historyService.getTrend(LocalDateTime.parse(from), LocalDateTime.parse(to), device);
    }
}

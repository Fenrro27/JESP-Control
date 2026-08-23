package com.Fenrro.JESP_Core.service;

import com.Fenrro.JESP_Core.entity.RelayHistory;
import com.Fenrro.JESP_Core.entity.SensorHistory;
import com.Fenrro.JESP_Core.repository.RelayHistoryRepository;
import com.Fenrro.JESP_Core.repository.SensorHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    public record TrendResult(double slopePerHour, double delta, String direction, long samples) {}

    private final SensorHistoryRepository sensorHistoryRepository;
    private final RelayHistoryRepository relayHistoryRepository;

    @Transactional
    public void insertSensorData(String deviceId, float temp, float hum) {
        sensorHistoryRepository.save(new SensorHistory(deviceId, temp, hum));
    }

    /** Registro legacy sin deviceId (compatibilidad). */
    @Transactional
    public void insertRelayEvent(int relayIndex, boolean state, String source) {
        relayHistoryRepository.save(new RelayHistory(relayIndex, state, source));
    }

    @Transactional
    public void insertRelayEvent(String deviceId, int relayIndex, boolean state, String source) {
        relayHistoryRepository.save(new RelayHistory(
                DeviceRegistry.DEFAULT_DEVICE_ID.equals(deviceId) ? null : deviceId,
                relayIndex, state, source));
    }

    @Transactional(readOnly = true)
    public List<SensorHistory> getRecentSensorHistory(String device, int limit) {
        return sensorHistoryRepository.findAllByOrderByIdDesc(PageRequest.of(0, limit))
                .stream()
                .filter(r -> matchesDevice(r.getDeviceId(), device))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorHistory> getSensorHistoryBetween(LocalDateTime from, LocalDateTime to,
                                                       String device, int limit) {
        return sensorHistoryRepository.findBetween(toEpochMillis(from), toEpochMillis(to),
                normalizeDeviceFilter(device), limit);
    }

    @Transactional(readOnly = true)
    public List<SensorHistoryRepository.HourlyStat> getHourlyProfile(LocalDateTime from,
                                                                     LocalDateTime to,
                                                                     String device) {
        return sensorHistoryRepository.findHourlyProfile(toEpochMillis(from), toEpochMillis(to),
                normalizeDeviceFilter(device));
    }

    @Transactional(readOnly = true)
    public SensorHistoryRepository.SummaryStat getSummary(LocalDateTime from, LocalDateTime to,
                                                          String device) {
        return sensorHistoryRepository.findSummary(toEpochMillis(from), toEpochMillis(to),
                normalizeDeviceFilter(device));
    }

    @Transactional(readOnly = true)
    public TrendResult getTrend(LocalDateTime from, LocalDateTime to, String device) {
        List<SensorHistory> rows =
                sensorHistoryRepository.findBetween(toEpochMillis(from), toEpochMillis(to),
                        (device == null || device.isBlank()) ? null : device, 10000);
        Collections.reverse(rows); // findBetween devuelve orden descendente por id
        if (rows.size() < 2) {
            return new TrendResult(0, 0, "sin datos", rows.size());
        }

        double xMean = 0, yMean = 0;
        for (SensorHistory r : rows) {
            xMean += r.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            yMean += r.getTemperature();
        }
        xMean /= rows.size();
        yMean /= rows.size();

        double num = 0, den = 0;
        for (SensorHistory r : rows) {
            double x = r.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - xMean;
            double y = r.getTemperature() - yMean;
            num += x * y;
            den += x * x;
        }
        double slopePerHour = den == 0 ? 0 : (num / den) * 3600.0 * 1000.0; // millis -> horas
        double delta = rows.get(rows.size() - 1).getTemperature() - rows.get(0).getTemperature();

        String direction;
        if (slopePerHour > 0.05) {
            direction = "subiendo";
        } else if (slopePerHour < -0.05) {
            direction = "bajando";
        } else {
            direction = "estable";
        }
        return new TrendResult(slopePerHour, delta, direction, rows.size());
    }

    private static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * null = sin filtro (incluye registros legacy). 'esp32-default' también
     * incluye los registros legacy con device_id NULL.
     */
    public static boolean matchesDevice(String rowDevice, String filter) {
        if (filter == null || filter.isBlank()) return true;
        if (rowDevice == null) {
            return DeviceRegistry.DEFAULT_DEVICE_ID.equals(filter);
        }
        return rowDevice.equals(filter);
    }

    private static String normalizeDeviceFilter(String device) {
        return (device == null || device.isBlank()) ? null : device;
    }
}
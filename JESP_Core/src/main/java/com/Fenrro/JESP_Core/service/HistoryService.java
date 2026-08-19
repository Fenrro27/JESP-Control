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
    public void insertSensorData(float temp, float hum) {
        sensorHistoryRepository.save(new SensorHistory(temp, hum));
    }

    @Transactional
    public void insertRelayEvent(int relayIndex, boolean state, String source) {
        relayHistoryRepository.save(new RelayHistory(relayIndex, state, source));
    }

    @Transactional(readOnly = true)
    public List<SensorHistory> getRecentSensorHistory(int limit) {
        return sensorHistoryRepository.findAllByOrderByIdDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<SensorHistory> getSensorHistoryBetween(LocalDateTime from, LocalDateTime to, int limit) {
        return sensorHistoryRepository.findBetween(toEpochMillis(from), toEpochMillis(to), limit);
    }

    @Transactional(readOnly = true)
    public List<SensorHistoryRepository.HourlyStat> getHourlyProfile(LocalDateTime from, LocalDateTime to) {
        return sensorHistoryRepository.findHourlyProfile(toEpochMillis(from), toEpochMillis(to));
    }

    @Transactional(readOnly = true)
    public SensorHistoryRepository.SummaryStat getSummary(LocalDateTime from, LocalDateTime to) {
        return sensorHistoryRepository.findSummary(toEpochMillis(from), toEpochMillis(to));
    }

    @Transactional(readOnly = true)
    public TrendResult getTrend(LocalDateTime from, LocalDateTime to) {
        List<SensorHistory> rows =
                sensorHistoryRepository.findBetween(toEpochMillis(from), toEpochMillis(to), 10000);
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
}
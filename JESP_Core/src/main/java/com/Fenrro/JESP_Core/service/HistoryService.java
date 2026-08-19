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
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

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

    private static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
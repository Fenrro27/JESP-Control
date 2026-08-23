package com.Fenrro.JESP_Core.repository;

import com.Fenrro.JESP_Core.entity.SensorHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SensorHistoryRepository extends JpaRepository<SensorHistory, Long> {

    List<SensorHistory> findAllByOrderByIdDesc(Pageable pageable);

    @Query(value = """
            SELECT * FROM sensor_history
            WHERE timestamp >= :fromMs AND timestamp <= :toMs
              AND (:device IS NULL OR device_id = :device
                   OR (device_id IS NULL AND :device = 'esp32-default'))
            ORDER BY id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SensorHistory> findBetween(@Param("fromMs") long fromMs, @Param("toMs") long toMs,
                                    @Param("device") String device,
                                    @Param("limit") int limit);

    @Query(value = """
            SELECT CAST(strftime('%H', timestamp / 1000.0, 'unixepoch', 'localtime') AS INTEGER) AS hour,
                   AVG(temperature) AS avgTemp,
                   MIN(temperature) AS minTemp,
                   MAX(temperature) AS maxTemp,
                   AVG(humidity) AS avgHum,
                   COUNT(*) AS records
            FROM sensor_history
            WHERE timestamp >= :fromMs AND timestamp <= :toMs
              AND (:device IS NULL OR device_id = :device
                   OR (device_id IS NULL AND :device = 'esp32-default'))
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<HourlyStat> findHourlyProfile(@Param("fromMs") long fromMs, @Param("toMs") long toMs,
                                       @Param("device") String device);

    @Query(value = """
            SELECT AVG(temperature) AS avgTemp,
                   MIN(temperature) AS minTemp,
                   MAX(temperature) AS maxTemp,
                   AVG(humidity) AS avgHum,
                   COUNT(*) AS records
            FROM sensor_history
            WHERE timestamp >= :fromMs AND timestamp <= :toMs
              AND (:device IS NULL OR device_id = :device
                   OR (device_id IS NULL AND :device = 'esp32-default'))
            """, nativeQuery = true)
    SummaryStat findSummary(@Param("fromMs") long fromMs, @Param("toMs") long toMs,
                            @Param("device") String device);

    interface HourlyStat {
        Integer getHour();

        Double getAvgTemp();

        Double getMinTemp();

        Double getMaxTemp();

        Double getAvgHum();

        Long getRecords();
    }

    interface SummaryStat {
        Double getAvgTemp();

        Double getMinTemp();

        Double getMaxTemp();

        Double getAvgHum();

        Long getRecords();
    }
}
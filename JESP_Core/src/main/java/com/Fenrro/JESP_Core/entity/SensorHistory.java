package com.Fenrro.JESP_Core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_history")
@Getter
@Setter
@NoArgsConstructor
public class SensorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /** Dispositivo de origen; null en registros legacy (esp32-default). */
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "temperature")
    private Float temperature;

    @Column(name = "humidity")
    private Float humidity;

    public SensorHistory(float temperature, float humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public SensorHistory(String deviceId, float temperature, float humidity) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @PrePersist
    void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
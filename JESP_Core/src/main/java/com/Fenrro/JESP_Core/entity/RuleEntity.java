package com.Fenrro.JESP_Core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "automation_rule")
@Getter
@Setter
@NoArgsConstructor
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    /** Dispositivo destino; null o vacío = dispositivo por defecto (esp32-default). */
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "relay_index", nullable = false)
    private Integer relayIndex;

    @Column(name = "target_state", nullable = false)
    private Boolean targetState = Boolean.TRUE;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Menor número = mayor prioridad. */
    @Column(name = "priority", nullable = false)
    private int priority = 100;

    /** Formato HH:mm, null = sin restricción temporal. */
    @Column(name = "time_start")
    private String timeStart;

    @Column(name = "time_end")
    private String timeEnd;

    @Column(name = "temp_min")
    private Float tempMin;

    @Column(name = "temp_max")
    private Float tempMax;

    @Column(name = "hum_min")
    private Float humMin;

    @Column(name = "hum_max")
    private Float humMax;

    /** AND / OR */
    @Column(name = "condition_logic", nullable = false)
    private String conditionLogic = "AND";

    /** CSV con MON,TUE,WED,THU,FRI,SAT,SUN; null = todos los días. */
    @Column(name = "days_of_week")
    private String daysOfWeek;

    /** Margen anti-flapping aplicado a los umbrales de temperatura/humedad. */
    @Column(name = "hysteresis")
    private Float hysteresis = 0.5f;

    /** Segundos mínimos entre conmutaciones automáticas del relé. 0 = sin límite. */
    @Column(name = "min_switch_interval_seconds", nullable = false)
    private int minSwitchIntervalSeconds = 0;

    /**
     * Evalúa si las condiciones de la regla se cumplen.
     *
     * @param hysteresis margen (>=0) que ensancha los umbrales de temperatura/humedad
     *                   para evitar flapping cuando la regla mantiene su estado.
     */
    public boolean evaluate(java.time.LocalTime now, float temp, float hum, float hysteresis) {
        boolean timeMatch = false;
        boolean timeChecked = false;
        if (timeStart != null && !timeStart.isBlank() && timeEnd != null && !timeEnd.isBlank()) {
            try {
                java.time.LocalTime start = java.time.LocalTime.parse(timeStart.trim());
                java.time.LocalTime end = java.time.LocalTime.parse(timeEnd.trim());
                timeChecked = true;
                if (start.isBefore(end)) {
                    timeMatch = !now.isBefore(start) && !now.isAfter(end);
                } else {
                    timeMatch = !now.isBefore(start) || !now.isAfter(end); // ventana nocturna
                }
            } catch (Exception e) {
                return false; // formato de hora inválido: la regla no dispara
            }
        }

        boolean tempMatch = false;
        boolean tempChecked = false;
        if (tempMin != null || tempMax != null) {
            tempChecked = true;
            tempMatch = true;
            if (tempMin != null && temp < tempMin - hysteresis) tempMatch = false;
            if (tempMax != null && temp > tempMax + hysteresis) tempMatch = false;
        }

        boolean humMatch = false;
        boolean humChecked = false;
        if (humMin != null || humMax != null) {
            humChecked = true;
            humMatch = true;
            if (humMin != null && hum < humMin - hysteresis) humMatch = false;
            if (humMax != null && hum > humMax + hysteresis) humMatch = false;
        }

        if ("OR".equalsIgnoreCase(conditionLogic)) {
            return (timeChecked && timeMatch) || (tempChecked && tempMatch)
                    || (humChecked && humMatch);
        }
        boolean result = true;
        if (timeChecked) result &= timeMatch;
        if (tempChecked) result &= tempMatch;
        if (humChecked) result &= humMatch;
        if (!timeChecked && !tempChecked && !humChecked) return false;
        return result;
    }
}

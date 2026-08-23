package com.Fenrro.JESP_Core.model;

import com.Fenrro.JESP_Core.entity.RuleEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser del formato legacy de rules.conf (CLAVE=VALOR separados por ---),
 * usado únicamente para la migración inicial a base de datos.
 */
@Slf4j
public final class LegacyRulesParser {

    private LegacyRulesParser() {}

    public static List<RuleEntity> parse(String content) {
        List<RuleEntity> rules = new ArrayList<>();
        RuleEntity current = null;

        for (String rawLine : content.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            if (line.equals("---")) {
                if (current != null && current.getRelayIndex() != null
                        && current.getRelayIndex() >= 0) {
                    // Las reglas migradas conservan el comportamiento exacto del motor antiguo
                    current.setHysteresis(0f);
                    rules.add(current);
                }
                current = null;
                continue;
            }
            if (current == null) current = new RuleEntity();

            String[] parts = line.split("=", 2);
            if (parts.length < 2) continue;
            String key = parts[0].trim().toUpperCase();
            String value = parts[1].trim();
            if (value.equals("-") || value.isEmpty()) continue;

            try {
                switch (key) {
                    case "RELAY" -> current.setRelayIndex(Integer.parseInt(value) - 1);
                    case "ACTION" -> current.setTargetState(value.equalsIgnoreCase("ON"));
                    case "TIME_START" -> current.setTimeStart(value);
                    case "TIME_END" -> current.setTimeEnd(value);
                    case "TEMP_MIN" -> current.setTempMin(Float.parseFloat(value));
                    case "TEMP_MAX" -> current.setTempMax(Float.parseFloat(value));
                    case "HUM_MIN" -> current.setHumMin(Float.parseFloat(value));
                    case "HUM_MAX" -> current.setHumMax(Float.parseFloat(value));
                    case "CONDITION_LOGIC" -> current.setConditionLogic(
                            value.equalsIgnoreCase("OR") ? "OR" : "AND");
                    default -> log.debug("Clave legacy ignorada: {}", key);
                }
            } catch (Exception e) {
                log.error("Error parseando línea de regla: {}", line);
            }
        }

        if (current != null && current.getRelayIndex() != null && current.getRelayIndex() >= 0) {
            current.setHysteresis(0f);
            rules.add(current);
        }
        return rules;
    }
}

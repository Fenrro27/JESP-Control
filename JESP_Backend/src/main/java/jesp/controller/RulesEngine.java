package jesp.controller;

import jesp.model.*;
import jesp.controller.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RulesEngine {
    private List<Rule> rules = new ArrayList<>();
    private Timer timer;

    public synchronized void loadRules(String filename) {
        rules.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            Rule currentRule = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.equals("---")) {
                    if (currentRule != null && currentRule.relayIndex != -1) {
                        rules.add(currentRule);
                    }
                    currentRule = null;
                    continue;
                }
                if (currentRule == null) currentRule = new Rule();
                
                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;
                String key = parts[0].trim().toUpperCase();
                String value = parts[1].trim();
                if (value.equals("-") || value.isEmpty()) continue;

                try {
                    switch (key) {
                        case "RELAY": currentRule.relayIndex = Integer.parseInt(value) - 1; break;
                        case "ACTION": currentRule.targetState = value.equalsIgnoreCase("ON"); break;
                        case "TIME_START": currentRule.timeStart = LocalTime.parse(value); break;
                        case "TIME_END": currentRule.timeEnd = LocalTime.parse(value); break;
                        case "TEMP_MIN": currentRule.tempMin = Float.parseFloat(value); break;
                        case "TEMP_MAX": currentRule.tempMax = Float.parseFloat(value); break;
                        case "HUM_MIN": currentRule.humMin = Float.parseFloat(value); break;
                        case "HUM_MAX": currentRule.humMax = Float.parseFloat(value); break;
                        case "CONDITION_LOGIC": currentRule.isAndLogic = value.equalsIgnoreCase("AND"); break;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing rule line: " + line);
                }
            }
            if (currentRule != null && currentRule.relayIndex != -1) {
                rules.add(currentRule);
            }
            System.out.println("Cargadas " + rules.size() + " reglas.");
        } catch (Exception e) {
            System.out.println("No se encontró o no se pudo cargar el archivo de reglas: " + filename + ". Usando modo manual.");
        }
        
        // Reevaluar inmediatamente al cargar o modificar reglas
        // Pasamos true para asumir estado inicial OFF si no hay reglas que digan lo contrario
        evaluateRules(true);
    }

    public void start() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                evaluateRules(false);
            }
        }, 0, 5000); // Evaluar cada 5 segundos
    }

    public synchronized void evaluateRules(boolean isReset) {
        LocalTime now = LocalTime.now();
        float temp = DeviceState.currentTemp;
        float hum = DeviceState.currentHum;

        Boolean[] desiredState = new Boolean[6];

        for (Rule rule : rules) {
            if (rule.relayIndex < 0 || rule.relayIndex >= 6) continue;
            if (DeviceState.overrideExpiration[rule.relayIndex] > System.currentTimeMillis()) continue; // No anular si está en manual y no ha expirado

            boolean conditionMet = rule.evaluate(temp, hum, now);
            if (conditionMet) {
                desiredState[rule.relayIndex] = rule.targetState;
            }
        }

        for (int i = 0; i < 6; i++) {
            if (DeviceState.overrideExpiration[i] > System.currentTimeMillis()) {
                continue;
            } else if (DeviceState.overrideExpiration[i] != 0) {
                // Si expiró, reseteamos a automático puro
                DeviceState.overrideExpiration[i] = 0;
            }

            boolean target;
            if (desiredState[i] != null) {
                target = desiredState[i];
            } else {
                if (isReset) {
                    target = false; // Estado inicial (OFF) al recargar reglas
                } else {
                    target = DeviceState.relays[i]; // Mantener estado actual (Histéresis)
                }
            }

            if (DeviceState.relays[i] != target) {
                DeviceState.setRelay(i, target);
                String reason = isReset ? "AUTOMATIC (RESET)" : "AUTOMATIC";
                DatabaseManager.insertRelayEvent(i, target, reason);
                System.out.println("Regla ejecutada: Rele " + (i + 1) + " cambiado a " + (target ? "ON" : "OFF"));
            }
        }
    }
}

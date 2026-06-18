package jesp;

import java.time.LocalTime;

public class Rule {
    public int relayIndex = -1;
    public boolean targetState = false;
    
    public LocalTime timeStart = null;
    public LocalTime timeEnd = null;
    
    public Float tempMin = null;
    public Float tempMax = null;
    
    public Float humMin = null;
    public Float humMax = null;
    
    public boolean isAndLogic = true; // true = AND, false = OR

    public boolean evaluate(float temp, float hum, LocalTime now) {
        boolean timeMatch = false;
        boolean timeChecked = false;
        if (timeStart != null && timeEnd != null) {
            timeChecked = true;
            if (timeStart.isBefore(timeEnd)) {
                timeMatch = !now.isBefore(timeStart) && !now.isAfter(timeEnd);
            } else { // Cruza la medianoche (ej: 22:00 a 06:00)
                timeMatch = !now.isBefore(timeStart) || !now.isAfter(timeEnd);
            }
        }

        boolean tempMatch = false;
        boolean tempChecked = false;
        if (tempMin != null || tempMax != null) {
            tempChecked = true;
            tempMatch = true;
            if (tempMin != null && temp < tempMin) tempMatch = false;
            if (tempMax != null && temp > tempMax) tempMatch = false;
        }

        boolean humMatch = false;
        boolean humChecked = false;
        if (humMin != null || humMax != null) {
            humChecked = true;
            humMatch = true;
            if (humMin != null && hum < humMin) humMatch = false;
            if (humMax != null && hum > humMax) humMatch = false;
        }

        if (isAndLogic) {
            boolean result = true;
            if (timeChecked) result &= timeMatch;
            if (tempChecked) result &= tempMatch;
            if (humChecked) result &= humMatch;
            // Si no hay condiciones definidas, devuelve falso
            if (!timeChecked && !tempChecked && !humChecked) return false;
            return result;
        } else {
            boolean result = false;
            if (timeChecked) result |= timeMatch;
            if (tempChecked) result |= tempMatch;
            if (humChecked) result |= humMatch;
            return result;
        }
    }
}

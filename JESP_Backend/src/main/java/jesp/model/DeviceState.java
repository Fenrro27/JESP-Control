package jesp.model;

import jesp.model.*;
import jesp.controller.*;

public class DeviceState {
    public static float currentTemp = 0.0f;
    public static float currentHum = 0.0f;
    public static boolean[] relays = new boolean[6];
    public static long[] overrideExpiration = new long[6]; // 0 = Auto, Long.MAX_VALUE = Perm, else Unix timestamp expiration
    
    // Callback para la GUI
    public static Runnable onStateChanged = null;
    public static Runnable onRelayChanged = null;

    public static synchronized void setTempAndHum(float temp, float hum) {
        currentTemp = temp;
        currentHum = hum;
        if (onStateChanged != null) onStateChanged.run();
    }

    public static synchronized void setRelay(int index, boolean state) {
        relays[index] = state;
        if (onStateChanged != null) onStateChanged.run();
        if (onRelayChanged != null) onRelayChanged.run();
    }
}

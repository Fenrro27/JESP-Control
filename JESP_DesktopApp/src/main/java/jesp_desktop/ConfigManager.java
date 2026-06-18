package jesp_desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties props = new Properties();

    static {
        load();
    }

    public static void load() {
        try {
            File f = new File(CONFIG_FILE);
            if (f.exists()) {
                props.load(new FileInputStream(f));
            }
        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }

    public static String getServerIp() {
        return props.getProperty("serverIp", "127.0.0.1:5001");
    }

    public static void setServerIp(String ip) {
        props.setProperty("serverIp", ip);
        try {
            props.store(new FileOutputStream(CONFIG_FILE), "JESP Desktop Configuration");
        } catch (Exception e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }
}

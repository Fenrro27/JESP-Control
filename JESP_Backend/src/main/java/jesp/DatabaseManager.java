package jesp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:jesp_data.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
             
            String sqlSensor = "CREATE TABLE IF NOT EXISTS sensor_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "temperature REAL," +
                    "humidity REAL" +
                    ");";
            stmt.execute(sqlSensor);

            String sqlRelay = "CREATE TABLE IF NOT EXISTS relay_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "relay_index INTEGER," +
                    "new_state BOOLEAN," +
                    "source TEXT" + // 'MANUAL' o 'AUTOMATIC'
                    ");";
            stmt.execute(sqlRelay);
            
            System.out.println("Base de datos SQLite inicializada correctamente.");
        } catch (Exception e) {
            System.err.println("Error inicializando base de datos: " + e.getMessage());
        }
    }

    public static void insertSensorData(float temp, float hum) {
        String sql = "INSERT INTO sensor_history(temperature, humidity) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setFloat(1, temp);
            pstmt.setFloat(2, hum);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error insertando datos de sensor: " + e.getMessage());
        }
    }

    public static void insertRelayEvent(int relayIndex, boolean state, String source) {
        String sql = "INSERT INTO relay_history(relay_index, new_state, source) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, relayIndex);
            pstmt.setBoolean(2, state);
            pstmt.setString(3, source);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error insertando evento de relé: " + e.getMessage());
        }
    }

    public static String getRecentSensorHistory(int limit) {
        String sql = "SELECT timestamp, temperature, humidity FROM sensor_history ORDER BY id DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"timestamp\":\"").append(rs.getString("timestamp")).append("\",")
                  .append("\"temp\":").append(rs.getFloat("temperature")).append(",")
                  .append("\"hum\":").append(rs.getFloat("humidity")).append("}");
                first = false;
            }
        } catch (Exception e) {
            System.err.println("Error consultando historial de sensores: " + e.getMessage());
        }
        sb.append("]");
        return sb.toString();
    }
}

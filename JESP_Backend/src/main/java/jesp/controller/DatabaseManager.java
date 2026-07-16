package jesp.controller;

import jesp.model.*;
import jesp.controller.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.security.MessageDigest;

public class DatabaseManager {
    private static String getDatabaseUrl() {
        String configuredPath = System.getenv("JESP_DB_PATH");
        String dbPath = (configuredPath != null && !configuredPath.isBlank()) ? configuredPath : "jesp_data.db";

        Path dbFile = Path.of(dbPath);
        Path parent = dbFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception e) {
                System.err.println("No se pudo crear el directorio de la base de datos: " + e.getMessage());
            }
        }

        return "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement()) {
             
            String sqlSensor = "CREATE TABLE IF NOT EXISTS sensor_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT (datetime('now', 'localtime'))," +
                    "temperature REAL," +
                    "humidity REAL" +
                    ");";
            stmt.execute(sqlSensor);

            String sqlRelay = "CREATE TABLE IF NOT EXISTS relay_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT (datetime('now', 'localtime'))," +
                    "relay_index INTEGER," +
                    "new_state BOOLEAN," +
                    "source TEXT" + // 'MANUAL' o 'AUTOMATIC'
                    ");";
            stmt.execute(sqlRelay);
            
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL," +
                    "role TEXT NOT NULL CHECK(role IN ('ADMIN', 'USER'))" +
                    ");";
            stmt.execute(sqlUsers);

            // Check if users table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM users");
            if (rs.next() && rs.getInt("count") == 0) {
                System.out.println("Tabla de usuarios vacía. Creando usuario admin por defecto...");
                addUser("admin", "admin", "ADMIN");
            }
            rs.close();
            
            System.out.println("Base de datos SQLite inicializada correctamente.");
        } catch (Exception e) {
            System.err.println("Error inicializando base de datos: " + e.getMessage());
        }
    }

    public static void insertSensorData(float temp, float hum) {
        String sql = "INSERT INTO sensor_history(timestamp, temperature, humidity) VALUES(datetime('now', 'localtime'), ?, ?)";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setFloat(1, temp);
            pstmt.setFloat(2, hum);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error insertando datos de sensor: " + e.getMessage());
        }
    }

    public static void insertRelayEvent(int relayIndex, boolean state, String source) {
        String sql = "INSERT INTO relay_history(timestamp, relay_index, new_state, source) VALUES(datetime('now', 'localtime'), ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
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
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
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

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean addUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.setString(3, role.toUpperCase());
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error añadiendo usuario: " + e.getMessage());
            return false;
        }
    }

    public static String authenticateAndGetRole(String username, String password) {
        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash.equals(hashPassword(password))) {
                    return rs.getString("role");
                }
            }
        } catch (Exception e) {
            System.err.println("Error autenticando usuario: " + e.getMessage());
        }
        return null;
    }
}

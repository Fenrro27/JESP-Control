package jesp;

import jesp.model.*;
import jesp.controller.*;

import java.io.File;

public class JespBackendMain {
    public static void main(String[] args) {
        String rulesFile = System.getenv().getOrDefault("JESP_RULES_FILE", "rules.conf");

        for (String arg : args) {
            if (arg.startsWith("--rules=")) {
                rulesFile = arg.substring("--rules=".length());
            }
        }

        System.out.println("=========================================");
        System.out.println(" Iniciando JESP-Control Backend...");
        System.out.println("=========================================");
        System.out.println("Ejecutando en modo Servidor (Headless).");
        System.out.println("Usando archivo de reglas: " + rulesFile);
        
        DatabaseManager.initialize();
        RulesEngine engine = new RulesEngine();
        
        // Crear un archivo de reglas de ejemplo si no existe
        File f = new File(rulesFile);
        if (!f.exists()) {
            crearReglasEjemplo(rulesFile);
        }
        
        engine.loadRules(rulesFile);
        engine.start();

        // Servidores HTTP y WebSocket
        Esp32Server server = new Esp32Server(rulesFile, engine);
        Esp32WebSocketServer wsServer = new Esp32WebSocketServer(5000);
        
        DeviceState.onRelayChanged = () -> {
            wsServer.broadcastCurrentConfig();
        };

        try {
            server.start();
            wsServer.start();
            System.out.println("Servidor WebSocket iniciado en puerto 5000.");
            System.out.println("APIs disponibles. Presiona Ctrl+C para detener.");
        } catch (Exception e) {
            System.err.println("Error crítico iniciando servidor HTTP/WS: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void crearReglasEjemplo(String filename) {
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(filename), 
                "# Archivo de configuración de reglas para JESP-Control\n" +
                "# Formato soportado por línea: CLAVE=VALOR\n" +
                "# Utiliza '---' para separar las reglas.\n" +
                "# CLAVES DISPONIBLES: RELAY (1-6), ACTION (ON/OFF), TIME_START (HH:mm), TIME_END (HH:mm), TEMP_MIN, TEMP_MAX, HUM_MIN, HUM_MAX, CONDITION_LOGIC (AND/OR)\n\n" +
                "# Ejemplo 1: Encender el relé 1 si la temperatura supera los 30 grados\n" +
                "RELAY=1\n" +
                "ACTION=ON\n" +
                "TEMP_MIN=30.0\n" +
                "CONDITION_LOGIC=AND\n" +
                "---\n\n" +
                "# Ejemplo 2: Encender el relé 2 todos los días de 18:00 a 22:00\n" +
                "RELAY=2\n" +
                "ACTION=ON\n" +
                "TIME_START=18:00\n" +
                "TIME_END=22:00\n" +
                "CONDITION_LOGIC=AND\n" +
                "---\n"
            );
            System.out.println("Archivo de reglas de ejemplo creado en: " + filename);
        } catch (Exception e) {
            System.out.println("No se pudo crear el archivo de reglas de ejemplo.");
        }
    }
}

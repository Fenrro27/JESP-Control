#include <WiFi.h>
#include <Wire.h>
#include "Adafruit_SHT31.h"
#include <WebSocketsClient.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>

// --- INCLUIR CREDENCIALES ---
#include "secrets.h"

// --- CONFIG WIFI ---
const char* ssid     = SECRET_SSID;
const char* password = SECRET_PASSWORD;

// --- CONFIG WEBSOCKET ---
const char* serverWSIP   = SECRET_SERVER_WS_IP;
const int   serverWSPort = SECRET_SERVER_WS_PORT;

WebSocketsClient webSocket;

// --- CONFIG SENSOR I2C ---
#define SDA_PIN 11
#define SCL_PIN 12
Adafruit_SHT31 sht30 = Adafruit_SHT31();

// --- CONFIG RELES ---
const int relePins[6] = {1, 2, 41, 42, 45, 46};
int estadoReles[6]    = {LOW, LOW, LOW, LOW, LOW, LOW};

// --- CONFIG BUZZER ---
const int buzzerPin = 21;

// --- VARIABLES DE DATOS ---
float ultimaTemp = 0;
float ultimaHum  = 0;

// --- GESTIÓN DE TIEMPOS ---
unsigned long tiempoSinWiFi = 0; // Almacena el momento exacto en que se perdió la conexión
unsigned long tiempoUltimoEnvioWS = 0;

// --- NTP CLIENT ---
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 3600, 60000); 

// --- FUNCIONES ---

void gestionarReles() {
  for (int i = 0; i < 6; i++) {
    digitalWrite(relePins[i], estadoReles[i]);
  }
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_DISCONNECTED:
      Serial.println("[WS] Desconectado!");
      if (tiempoSinWiFi == 0) {
        tiempoSinWiFi = millis(); // Empezar a contar el timeout de seguridad (3 mins)
      }
      break;
    case WStype_CONNECTED:
      Serial.printf("[WS] Conectado a url: %s\n", payload);
      tiempoSinWiFi = 0; // Resetear timeout de seguridad
      break;
    case WStype_TEXT:
      {
        String msg = (char*)payload;
        DynamicJsonDocument doc(512);
        DeserializationError error = deserializeJson(doc, msg);
        if (!error && doc.containsKey("reles")) {
          for (int i = 0; i < 6; i++) {
            estadoReles[i] = doc["reles"][i] ? HIGH : LOW;
          }
          gestionarReles();
        }
      }
      break;
    case WStype_BIN:
    case WStype_ERROR:      
    case WStype_FRAGMENT_TEXT_START:
    case WStype_FRAGMENT_BIN_START:
    case WStype_FRAGMENT:
    case WStype_FRAGMENT_FIN:
      break;
  }
}

void enviarDatos() {
  if (WiFi.status() != WL_CONNECTED) return;
  // Enviar los datos por WebSocket si está conectado
  DynamicJsonDocument doc(256);
  doc["temp"] = ultimaTemp;
  doc["hum"] = ultimaHum;
  
  String output;
  serializeJson(doc, output);
  webSocket.sendTXT(output);
}

// --- SETUP ---
void setup() {
  Serial.begin(115200);
  Wire.begin(SDA_PIN, SCL_PIN);
  sht30.begin(0x44);

  for (int i = 0; i < 6; i++) {
    pinMode(relePins[i], OUTPUT);
    digitalWrite(relePins[i], LOW); 
  }
  
  pinMode(buzzerPin, OUTPUT);

  // Intentar la primera conexión
  WiFi.begin(ssid, password);
  timeClient.begin();

  // Configurar WebSocket
  webSocket.begin(serverWSIP, serverWSPort, "/");
  webSocket.onEvent(webSocketEvent);
  // Intentar reconectar cada 5 segundos si se cae
  webSocket.setReconnectInterval(5000);

  // Tono de inicio
  tone(buzzerPin, 1000, 200);
  delay(250);
}

// --- LOOP ---
void loop() {
  // Lectura constante de sensores
  ultimaTemp = sht30.readTemperature();
  ultimaHum  = sht30.readHumidity();

  // Atender peticiones del WebSocket
  webSocket.loop();

  // --- LÓGICA DE CONTROL DE INTERNET Y ALARMAS ---
  if (WiFi.status() != WL_CONNECTED) {
    if (tiempoSinWiFi == 0) {
      tiempoSinWiFi = millis();
    }
  }

  if (tiempoSinWiFi > 0) {
    // 1. Pitar de vez en cuando (Cada 5 segundos de forma no bloqueante)
    static unsigned long ultimoPitido = 0;
    if (millis() - ultimoPitido > 5000) {
      tone(buzzerPin, 800, 150); 
      ultimoPitido = millis();
    }

    // 2. Si pasan más de 3 minutos sin internet -> APAGAR (Deep Sleep)
    if (millis() - tiempoSinWiFi > 3UL * 60UL * 1000UL) {
      for (int i = 0; i < 6; i++) {
        digitalWrite(relePins[i], LOW);
      }
      esp_sleep_enable_timer_wakeup(30ULL * 60ULL * 1000ULL * 1000ULL); 
      esp_deep_sleep_start();
    }

    // 3. Intentar reconectar WiFi en segundo plano cada 15 segundos
    static unsigned long ultimoIntentoWiFi = 0;
    if (millis() - ultimoIntentoWiFi > 15000 && WiFi.status() != WL_CONNECTED) {
      WiFi.disconnect();
      WiFi.begin(ssid, password);
      ultimoIntentoWiFi = millis();
    }
  } else {
    // Hay WiFi (el webSocketEvent se encarga de poner tiempoSinWiFi = 0 cuando WS conecta)
    
    static unsigned long ultimoNTP = 0;
    if (millis() - ultimoNTP > 15000) {
      timeClient.update();
      ultimoNTP = millis();
    }

    // Lógica de Delta Reporting: Enviar si cambia >= 0.1 grados/humedad o pasan 60s (Heartbeat)
    static float ultimaTempEnviada = -999.0;
    static float ultimaHumEnviada = -999.0;

    float deltaTemp = abs(ultimaTemp - ultimaTempEnviada);
    float deltaHum = abs(ultimaHum - ultimaHumEnviada);
    bool heartbeat = (millis() - tiempoUltimoEnvioWS > 60000);

    if (heartbeat || deltaTemp >= 0.1 || deltaHum >= 0.1) {
      enviarDatos();
      tiempoUltimoEnvioWS = millis();
      ultimaTempEnviada = ultimaTemp;
      ultimaHumEnviada = ultimaHum;
    }
  }

  delay(200);
}
# JESP-Control - Firmware y Emulación

Este directorio contiene los programas relacionados con el hardware ESP32, incluyendo el firmware real y un emulador para pruebas.

## Programas

### 1. `ESP32_Control` (Firmware C++)
Firmware diseñado específicamente para el **ESP32 Waveshare Industrial 6-channel ESP32-S3 WiFi Relay Module**.

**Características de Hardware:**
- Microcontrolador ESP32-S3
- Módulo de 6 relés
- Sensor de Temperatura y Humedad SHT30 por I2C (Pines SDA: 11, SCL: 12)

El programa de Arduino se conecta mediante WiFi (WebSockets) al servidor backend en Java para recibir comandos y accionar los relés, además de enviar telemetría de sensores periódicamente.

#### Configuración Obligatoria (`secrets.h`)
Antes de compilar el código, es **obligatorio** crear un archivo `secrets.h` dentro del directorio `ESP32_Control/` (o modificarlo si ya existe). Este archivo almacena información sensible y no debe compartirse públicamente.
El contenido del archivo debe tener exactamente esta estructura:
```cpp
#ifndef SECRETS_H
#define SECRETS_H

// --- CREDENCIALES WI-FI SECRETA ---
const char* SECRET_SSID     = "NOMBRE_DE_TU_RED_WIFI";
const char* SECRET_PASSWORD = "CONTRASEÑA_DE_TU_WIFI";

// --- WEBSOCKET DEL SERVIDOR ---
// Reemplaza '192.168.0.251' por la IP local de la máquina donde se ejecuta el backend Java
const char* SECRET_SERVER_WS_IP      = "192.168.0.251";
const int SECRET_SERVER_WS_PORT      = 5000;

#endif
```

### 2. `ESP32_Emu` (Emulador Python)
Un script de Python (`ESP32_Emu.py`) que simula el hardware real del ESP32. Esto es extremadamente útil para desarrollar o probar el `JESP_Core` y `JESP_DesktopApp` cuando no se dispone del módulo físico.

**Características del Emulador:**
- Simula lecturas graduales de temperatura y humedad (con variaciones y redondeos realistas).
- Se conecta al servidor backend vía WebSocket de la misma forma que el hardware real.
- Emula la recepción y aplicación de estados a los 6 relés (imprimiendo por consola).
- Simula "Deep Sleep" y reconexiones automáticas tras pérdida de conexión.

**Ejecución del Emulador:**
Asegúrate de tener instalada la librería WebSocket y ejecuta el script:
```bash
pip install websocket-client
python ESP32_Emu/ESP32_Emu.py
```

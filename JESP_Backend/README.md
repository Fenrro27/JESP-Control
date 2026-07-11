# JESP-Control - Backend Java

Este directorio contiene la aplicación de servidor principal (headless) en Java para el proyecto JESP-Control.
Su propósito principal es comunicarse y controlar el **ESP32 Waveshare Industrial 6-channel ESP32-S3 WiFi Relay Module**, así como proveer servicio a clientes como la aplicación de escritorio.

## Descripción

El servidor actúa como el controlador central del sistema:
- **Gestión de Hardware:** Se comunica con el ESP32 (o su emulador) a través de WebSockets y HTTP para recibir telemetría (temperatura, humedad) y enviar comandos a los relés.
- **Motor de Reglas (RulesEngine):** Evalúa un archivo de reglas configurables (`rules.conf`) para automatizar el encendido/apagado de los relés en función de las lecturas de los sensores.
- **Base de Datos:** Utiliza SQLite (`jesp_data.db`) para almacenar un registro histórico del estado del sistema.

## Requisitos

- Java Development Kit (JDK) 11 o superior.
- Maven (para gestión de dependencias y compilación).
- Entorno de desarrollo compatible con proyectos Java (Eclipse, IntelliJ IDEA, etc.).

## Ejecución

Puedes ejecutar el servidor directamente desde tu IDE ejecutando la clase `jesp.JespBackendMain`, o compilando el proyecto con Maven.
Por defecto, el backend buscará el archivo `rules.conf` en el directorio de ejecución y levantará servicios en **dos puertos**:
- **Puerto 5000 (WebSocket):** Utilizado por el hardware ESP32 (y el emulador) para enviar telemetría en tiempo real, mantener una conexión abierta y recibir cambios de estado de los relés de forma instantánea.
- **Puerto 5001 (HTTP API):** Utilizado por las aplicaciones cliente (Desktop App) para consultar el estado, forzar el estado de relés y gestionar reglas.

### Lanzar el backend con Docker

Desde la carpeta del backend, ejecuta:

```bash
docker compose up --build
```

Esto construye la imagen, levanta el contenedor y monta un volumen persistente para guardar `jesp_data.db` y `rules.conf` en `/app/data`.

Con esto, el contenedor arranca el backend automáticamente y la base de datos SQLite queda persistida entre reinicios.

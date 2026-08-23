# JESP-Control

Este proyecto es un sistema integral diseñado para controlar un **ESP32 Waveshare Industrial 6-channel ESP32-S3 WiFi Relay Module** (con interfaces RS485 integradas y compatibilidad con Pico HAT). El sistema cuenta con servidor backend, aplicación de escritorio, app web móvil y herramientas de simulación.

## Estructura del Proyecto

- [`JESP_Core/`](./JESP_Core/): Servidor backend en Java con **Spring Boot**. Gestiona la base de datos (SQLite), evalúa las reglas automáticas (`RulesEngine`) y sirve de puente mediante HTTP y WebSockets con los dispositivos ESP32 y las aplicaciones cliente. Incluye **autenticación JWT con roles**, soporte **multi-dispositivo** e **integración con Home Assistant** vía su API.
- [`JESP_MobileApp/`](./JESP_MobileApp/): Aplicación web móvil (SPA en **React 18 + Vite**) para monitorizar y controlar el sistema desde el navegador del móvil o PC.
- [`JESP_DesktopApp/`](./JESP_DesktopApp/): Aplicación de escritorio con interfaz gráfica (GUI) en Java. Se conecta al backend con login JWT para monitorizar sensores, forzar relés y gestionar reglas.
- [`arduino/ESP32_Control/`](./arduino/ESP32_Control/): Firmware en C++ para el módulo de relés ESP32-S3 de Waveshare.
- [`arduino/ESP32_Emu/`](./arduino/ESP32_Emu/): Emulador Python del hardware para desarrollar sin el módulo físico.

## Autenticación

El backend usa **Spring Security + JWT stateless**:

| Rol | Permisos |
|---|---|
| `USER` | Consulta de estado, historial, estadísticas y reglas |
| `ADMIN` | Todo lo anterior + control de relés, CRUD de reglas y gestión de usuarios |

- En el primer arranque se crea el usuario `admin`. Su contraseña se toma de la variable de entorno `JESP_ADMIN_PASSWORD`; si no está definida, se genera una aleatoria que se imprime en el log.
- Configuración relevante: `JESP_JWT_SECRET` (mínimo 32 caracteres), `JESP_JWT_EXPIRATION_MINUTES`, `JESP_CORS_ORIGINS`.
- Endpoint de login: `POST /api/auth/login` → `{token, username, role}`. Las peticiones autenticadas incluyen la cabecera `Authorization: Bearer <token>`.

## Multi-dispositivo

El WebSocket del backend (puerto 5000) admite varios dispositivos simultáneos:

- Un dispositivo puede identificarse enviando `{"id": "mi-dispositivo", "temp": …, "hum": …}` en cualquier mensaje WS.
- Las conexiones **sin identificador** se asignan al dispositivo por defecto `esp32-default`, por lo que **el firmware actual sigue funcionando sin cambios** (compatibilidad hacia atrás).
- API por dispositivo: `GET /api/devices`, `GET /api/devices/{id}/state`, `POST /api/devices/{id}/relay`, `POST /api/devices/{id}/reset_override`.
- Los endpoints legacy (`/api/state`, `/api/relay`) siguen operando sobre `esp32-default`.
- Cada regla puede dirigirse a un dispositivo concreto mediante su campo `deviceId` (vacío = dispositivo por defecto).

## Motor de reglas reforzado

Las reglas viven ahora en la base de datos (el antiguo `rules.conf` se importa automáticamente en el primer arranque y se respalda como `rules.conf.migrated.bak`). Novedades respecto al motor original:

- **Histéresis**: evita el flapping de relés alrededor de los umbrales de temperatura/humedad.
- **Prioridad**: ante reglas conflictivas sobre el mismo relé gana la de menor valor numérico.
- **Cooldown**: intervalo mínimo entre conmutaciones automáticas por regla (`minSwitchIntervalSeconds`).
- **Días de semana**: restricción opcional a días concretos (`MON,TUE,…`).
- **Failsafe por sensor obsoleto**: si el dispositivo deja de reportar datos durante `JESP_SENSOR_STALENESS` segundos, las reglas dependientes de sensores se suspenden y los relés que gobiernan se apagan (`JESP_STALE_FAILSAFE=OFF`) o mantienen (`KEEP`). Las reglas solo-horario siguen funcionando sin sensor.
- **Validación**: la API rechaza reglas incoherentes (índice fuera de rango, rangos invertidos, horas mal formateadas, reglas sin condiciones…).

Gestión vía API JSON: `GET/POST /api/rules`, `PUT/DELETE /api/rules/{id}`, `POST /api/rules/evaluate`.

## Integración con Home Assistant

Hay dos caminos posibles:

### Opción recomendada: MQTT (auto-discovery nativo)

1. Instala un broker MQTT (p. ej. Mosquitto) y añade a `JESP_Core` un publicador/suscriptor MQTT (Eclipse Paho o spring-integration-mqtt).
2. El backend publica estado: `jesp/device/{id}/sensor/temp`, `.../hum`, `jesp/device/{id}/relay/{n}/state`.
3. El backend se suscribe a comandos: `jesp/device/{id}/relay/{n}/set` (`ON`/`OFF`) aplicándolos como overrides manuales.
4. Publicando mensajes de auto-discovery (`homeassistant/switch/jesp/rele_1/config`, `homeassistant/sensor/jesp/temp/config`…) los dispositivos aparecen solos en HA, listos para automaciones, tarjetas del dashboard y asistentes de voz.
5. Tus reglas siguen decidiendo en el motor propio; HA observa y puede forzar overrides.

### Opción rápida: REST (sin tocar el backend)

Con el JWT ya puedes integrarte hoy desde `configuration.yaml` de HA:

```yaml
rest_command:
  jesp_rele:
    url: "http://IP_SERVIDOR:5001/api/relay?relay={{ relay }}&state={{ state }}"
    method: POST
    headers:
      Authorization: "Bearer !secret jesp_token"

rest:
  - resource: "http://IP_SERVIDOR:5001/api/state"
    headers:
      Authorization: "Bearer !secret jesp_token"
    sensor:
      - name: "JESP Temperatura"
        value_template: "{{ value_json.temp }}"
        unit_of_measurement: "°C"
```

Funciona, pero es sondeo periódico y menos idiomático en HA; para producción se recomienda MQTT.

## Ejecución rápida

```bash
# Backend
cd JESP_Core
./mvnw spring-boot:run          # API en :5001, WS dispositivos en :5000

# App móvil (desarrollo)
cd JESP_MobileApp && npm install && npm run dev   # http://localhost:5173

# App móvil (producción: build dentro del backend)
cd JESP_MobileApp && npm run build
cd ../JESP_Core && ./mvnw spring-boot:run         # SPA servida en http://localhost:5001/

# Docker
cd JESP_Core && docker compose up -d --build
```

Variables de entorno principales: `JESP_DB_PATH`, `JESP_RULES_FILE`, `JESP_WS_PORT`, `JESP_JWT_SECRET`, `JESP_ADMIN_PASSWORD`, `JESP_SENSOR_STALENESS`, `JESP_STALE_FAILSAFE`.

Por favor, consulta los archivos `README.md` específicos dentro de cada directorio para más detalles.

## Mejoras implementadas

- ✅ Autenticación JWT con roles ADMIN/USER en toda la API.
- ✅ Reglas persistentes en base de datos con validación y CRUD JSON.
- ✅ Motor de reglas reforzado (histéresis, prioridad, cooldown, días de semana, failsafe por sensor obsoleto).
- ✅ Soporte multi-dispositivo compatible con el firmware actual.
- ✅ App web móvil en React con dashboard, historial, estadísticas, reglas y usuarios.
- ✅ Historial gráfico y estadísticas con tendencia de temperatura.

## Futuras Mejoras Propuestas

Dado que la arquitectura centraliza la lógica en el backend Java, el sistema es altamente escalable:

- **📡 Firmware v2 con identificación**: actualizar `ESP32_Control.ino` para enviar su `deviceId` (p. ej. derivado de la MAC) y aprovechar el multi-dispositivo real; requerirá reflashear el Arduino.
- **🔒 Token compartido en el WS del ESP32**: proteger el puerto 5000 con token por query/header durante el handshake (hoy confía en la red local).
- **🏠 Integración completa con Home Assistant vía MQTT**: broker + auto-discovery según lo descrito arriba.
- **🤖 Integración con Bots (Telegram/WhatsApp)**: alertas instantáneas y control remoto sin abrir puertos.
- **🌤️ Clima Exterior Predictivo (API Open-Meteo)**: enriquecer el motor de reglas con previsión meteorológica gratuita (ej. evitar riego si hay lluvia prevista).
- **⏱️ Programación Avanzada (Cron Jobs)**: ciclos complejos y eventos recurrentes precisos (ej. "cada primer lunes de mes").
- **🔒 Enclavamientos de Seguridad (Interlocks)**: prevención software de activar relés conflictivos simultáneamente.
- **📊 Dashboards Avanzados con Grafana**: conectar `jesp_data.db` a Grafana para analítica en tiempo real.

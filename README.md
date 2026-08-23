# JESP-Control

Este proyecto es un sistema integral diseñado para controlar un **ESP32 Waveshare Industrial 6-channel ESP32-S3 WiFi Relay Module** (con interfaces RS485 integradas y compatibilidad con Pico HAT). El sistema cuenta con múltiples componentes para el hardware, servidor de backend, aplicación de escritorio y herramientas de simulación.

## Estructura del Proyecto

El proyecto está compuesto por los siguientes programas y directorios principales:

- [`JESP_Core/`](./JESP_Core/): Servidor backend en Java con Spring Boot. Se encarga de gestionar la base de datos (SQLite), evaluar las reglas automáticas (`RulesEngine`) y servir de puente mediante HTTP y WebSockets con los dispositivos ESP32 y las aplicaciones cliente.
- [`JESP_DesktopApp/`](./JESP_DesktopApp/): Aplicación de escritorio con interfaz gráfica (GUI) en Java. Permite a los usuarios conectarse al backend para monitorizar sensores, forzar el estado de los relés manualmente y visualizar los datos del sistema.
- [`arduino/ESP32_Control/`](./arduino/ESP32_Control/): Firmware en C++ para el módulo de relés ESP32-S3 de Waveshare. Lee los sensores físicos (ej. SHT30) y se comunica con el backend mediante WebSockets.
- [`arduino/ESP32_Emu/`](./arduino/ESP32_Emu/): Script de Python (`ESP32_Emu.py`) que emula el comportamiento del hardware físico del ESP32. Permite probar y desarrollar el backend y la aplicación de escritorio sin necesidad de tener el hardware físico conectado.

Por favor, consulta los archivos `README.md` específicos dentro de cada directorio para obtener más información sobre la configuración y ejecución de cada programa.

## Futuras Mejoras Propuestas

Dado que la arquitectura centraliza la lógica en el backend Java, el sistema es altamente escalable. Algunas mejoras posibles para futuras versiones incluyen:

- **🤖 Integración con Bots (Telegram/WhatsApp):** Alertas instantáneas de sensores y control remoto de relés mediante mensajes, sin necesidad de abrir puertos en el router.
- **🌤️ Clima Exterior Predictivo (API Open-Meteo):** Enriquecer el motor de reglas con datos meteorológicos externos gratuitos (ej. evitar el riego si hay previsión de lluvia).
- **🏡 Integración con Home Assistant / MQTT:** Publicar el estado en un broker MQTT para integrarse fácilmente con ecosistemas domóticos y usar asistentes de voz como Alexa o Google Home.
- **⏱️ Programación Avanzada (Cron Jobs):** Integrar librerías como Quartz Scheduler para programar ciclos complejos y eventos recurrentes precisos (ej. "cada primer lunes de mes").
- **🔒 Enclavamientos de Seguridad (Interlocks):** Prevención por software de la activación simultánea de relés conflictivos (ej. activar motor hacia adelante y hacia atrás a la vez).
- **📊 Dashboards Avanzados con Grafana:** Conectar la base de datos SQLite (`jesp_data.db`) a Grafana para generar gráficos analíticos en tiempo real del historial de temperatura y uso de relés.
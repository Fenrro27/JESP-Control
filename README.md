# JESP-Control

Este proyecto es un sistema integral diseñado para controlar un **ESP32 Waveshare Industrial 6-channel ESP32-S3 WiFi Relay Module** (con interfaces RS485 integradas y compatibilidad con Pico HAT). El sistema cuenta con múltiples componentes para el hardware, servidor de backend, aplicación de escritorio y herramientas de simulación.

## Estructura del Proyecto

El proyecto está compuesto por los siguientes programas y directorios principales:

- [`JESP_Backend/`](./JESP_Backend/): Servidor backend en Java (Headless). Se encarga de gestionar la base de datos (SQLite), evaluar las reglas automáticas (`RulesEngine`) y servir de puente mediante HTTP y WebSockets con los dispositivos ESP32 y las aplicaciones cliente.
- [`JESP_DesktopApp/`](./JESP_DesktopApp/): Aplicación de escritorio con interfaz gráfica (GUI) en Java. Permite a los usuarios conectarse al backend para monitorizar sensores, forzar el estado de los relés manualmente y visualizar los datos del sistema.
- [`arduino/ESP32_Control/`](./arduino/ESP32_Control/): Firmware en C++ para el módulo de relés ESP32-S3 de Waveshare. Lee los sensores físicos (ej. SHT30) y se comunica con el backend mediante WebSockets.
- [`arduino/ESP32_Emu/`](./arduino/ESP32_Emu/): Script de Python (`ESP32_Emu.py`) que emula el comportamiento del hardware físico del ESP32. Permite probar y desarrollar el backend y la aplicación de escritorio sin necesidad de tener el hardware físico conectado.

Por favor, consulta los archivos `README.md` específicos dentro de cada directorio para obtener más información sobre la configuración y ejecución de cada programa.
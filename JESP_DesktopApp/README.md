# JESP-Control - Desktop Application

Este directorio contiene la aplicación cliente con interfaz gráfica (GUI) desarrollada en Java para controlar el sistema JESP-Control de forma visual.

## Descripción

La aplicación de escritorio se conecta de forma remota al `JESP_Core` a través de su API HTTP para proveer un panel de control interactivo (Dashboard). Sus funcionalidades principales incluyen:
- **Monitorización en Tiempo Real:** Visualización de la temperatura y humedad actuales recibidas desde el ESP32 (o su emulador).
- **Control de Relés:** Permite encender o apagar manualmente cada uno de los 6 relés del módulo industrial.
- **Sincronización:** Mantiene su interfaz gráfica actualizada haciendo peticiones continuas al backend para reflejar cambios ocurridos por reglas automáticas u otros clientes.

## Requisitos

- Java Development Kit (JDK) 11 o superior.
- Maven (para gestión de dependencias y compilación).

## Ejecución

1. Asegúrate de que el servidor `JESP_Core` esté en ejecución.
2. Compila y ejecuta el proyecto `JESP_DesktopApp` desde tu IDE (ejecutando la clase principal `jesp_desktop.DesktopMain`) o mediante Maven.
3. Al iniciar se mostrará un **diálogo de login**: introduce un usuario válido del backend (`admin` por defecto; la contraseña se configura con `JESP_ADMIN_PASSWORD` en el servidor). El rol `ADMIN` permite controlar relés y gestionar reglas; el rol `USER` solo consulta.
4. La aplicación intentará conectarse mediante HTTP a la dirección del servidor configurada (por defecto a `127.0.0.1:5001` consumiendo la API de escritorio).
5. La pestaña **Reglas** gestiona las reglas de automatización contra la API JSON: crear, editar, activar/desactivar y eliminar, con soporte de histéresis, prioridad, cooldown y días de semana.

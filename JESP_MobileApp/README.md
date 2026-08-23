# JESP_MobileApp

Aplicación web móvil (SPA en React 18 + Vite) para controlar JESP-Control desde el navegador del móvil o del PC.

## Funcionalidades

- **Login** con JWT (roles `ADMIN` y `USER`).
- **Panel**: temperatura/humedad en tiempo real (polling 3 s), control de los 6 relés con indicador de override manual y botón para devolver el control a las reglas.
- **Selector de dispositivo**: si hay varios ESP32 conectados al servidor, se puede cambiar entre ellos.
- **Historial**: gráfica de temperatura/humedad (6 h / 24 h / 7 días).
- **Estadísticas**: resumen numérico y tendencia de las últimas 24 h.
- **Reglas** (solo ADMIN): CRUD completo con prioridad, histéresis, cooldown, días de semana, lógica AND/OR y condiciones por sensor u horario.
- **Usuarios** (solo ADMIN): creación, listado y borrado.

## Desarrollo

```bash
cd JESP_MobileApp
npm install
npm run dev        # http://localhost:5173 (proxy /api -> localhost:5001)
```

Requiere que `JESP_Core` esté en ejecución en el puerto 5001.

## Producción

El build genera la SPA directamente dentro de los recursos estáticos de Spring Boot:

```bash
npm run build      # -> ../JESP_Core/src/main/resources/static
```

Después, arranca `JESP_Core` normalmente: la app se sirve en `http://IP_SERVIDOR:5001/` con la API en el mismo puerto (sin CORS).

> En móvil, la primera vez conviene usar "Añadir a pantalla de inicio" desde el navegador para una experiencia tipo app.

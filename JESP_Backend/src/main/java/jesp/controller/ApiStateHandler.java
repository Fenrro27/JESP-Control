package jesp.controller;

import jesp.model.DeviceState;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ApiStateHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"temp\": ").append(DeviceState.currentTemp)
          .append(", \"hum\": ").append(DeviceState.currentHum)
          .append(", \"relays\": [");
        for (int i = 0; i < 6; i++) {
            sb.append(DeviceState.relays[i]);
            if (i < 5) sb.append(", ");
        }
        sb.append("], \"overrides\": [");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            sb.append(DeviceState.overrideExpiration[i] > now);
            if (i < 5) sb.append(", ");
        }
        sb.append("]}");

        resp.setContentType("application/json");
        resp.setStatus(200);
        resp.getWriter().write(sb.toString());
    }
}

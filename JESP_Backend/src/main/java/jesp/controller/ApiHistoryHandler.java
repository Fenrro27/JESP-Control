package jesp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ApiHistoryHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int limit = 100;
        String limitParam = req.getParameter("limit");
        if (limitParam != null) {
            try { limit = Integer.parseInt(limitParam); } catch (Exception e) {}
        }
        
        String responseStr = DatabaseManager.getRecentSensorHistory(limit);
        resp.setContentType("application/json");
        resp.setStatus(200);
        resp.getWriter().write(responseStr);
    }
}

package jesp_desktop;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.List;

public class Charts {

    public static ChartPanel createHistoryChart(List<BackendClient.HistoryPoint> points, long fromMillis, long toMillis) {
        XYSeries tempSeries = new XYSeries("Temperatura (°C)");
        XYSeries humSeries = new XYSeries("Humedad (%)");
        for (BackendClient.HistoryPoint p : points) {
            tempSeries.add(p.timestampMillis, p.temp);
            humSeries.add(p.timestampMillis, p.hum);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(tempSeries);
        dataset.addSeries(humSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Historial de Sensores",
                "Fecha/Hora", "Valor",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        renderer.setDefaultStroke(new java.awt.BasicStroke(2.0f));
        plot.setRenderer(renderer);

        DateAxis axis = new DateAxis("Fecha/Hora");
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        axis.setRange(fromMillis, toMillis);
        plot.setDomainAxis(axis);

        return new ChartPanel(chart);
    }

    public static ChartPanel createHourlyProfileChart(List<BackendClient.HourStat> current, List<BackendClient.HourStat> previous) {
        XYSeries currentSeries = new XYSeries("Últimos 30 días");
        for (BackendClient.HourStat s : current) {
            if (s.avgTemp != null) {
                currentSeries.add(s.hour, s.avgTemp);
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(currentSeries);

        JFreeChart chart;
        if (hasData(previous)) {
            XYSeries previousSeries = new XYSeries("Año anterior");
            for (BackendClient.HourStat s : previous) {
                if (s.avgTemp != null) {
                    previousSeries.add(s.hour, s.avgTemp);
                }
            }
            dataset.addSeries(previousSeries);
        }

        chart = ChartFactory.createXYLineChart(
                "Temperatura media por hora del día",
                "Hora del día", "Temperatura (°C)",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultStroke(new java.awt.BasicStroke(2.0f));
        plot.setRenderer(renderer);

        NumberAxis hourAxis = new NumberAxis("Hora del día");
        hourAxis.setRange(0, 23);
        hourAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.setDomainAxis(hourAxis);

        return new ChartPanel(chart);
    }

    private static boolean hasData(List<BackendClient.HourStat> stats) {
        for (BackendClient.HourStat s : stats) {
            if (s.avgTemp != null) {
                return true;
            }
        }
        return false;
    }
}
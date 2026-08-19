package jesp_desktop;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.List;

public class Charts {

    public static ChartPanel createHistoryChart() {
        XYSeries tempSeries = new XYSeries("Temperatura (°C)");
        XYSeries humSeries = new XYSeries("Humedad (%)");
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(tempSeries);
        dataset.addSeries(humSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Historial de Sensores (últimas 24 horas)",
                "Fecha/Hora", "Valor",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        renderer.setDefaultStroke(new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        DateAxis axis = new DateAxis("Fecha/Hora");
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        plot.setDomainAxis(axis);

        return new ChartPanel(chart);
    }

    public static void updateHistoryChart(ChartPanel panel, List<BackendClient.HistoryPoint> points,
                                         long fromMillis, long toMillis) {
        XYPlot plot = panel.getChart().getXYPlot();
        XYSeries tempSeries = new XYSeries("Temperatura (°C)");
        XYSeries humSeries = new XYSeries("Humedad (%)");
        for (BackendClient.HistoryPoint p : points) {
            tempSeries.add(p.timestampMillis, p.temp);
            humSeries.add(p.timestampMillis, p.hum);
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(tempSeries);
        dataset.addSeries(humSeries);
        plot.setDataset(dataset);
        ((DateAxis) plot.getDomainAxis()).setRange(fromMillis, toMillis);
    }

    public static ChartPanel createHourlyProfileChart() {
        XYSeries currentSeries = new XYSeries("Últimos 30 días");
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(currentSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Temperatura media por hora del día",
                "Hora del día", "Temperatura (°C)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultStroke(new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        NumberAxis hourAxis = new NumberAxis("Hora del día");
        hourAxis.setRange(0, 23);
        hourAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.setDomainAxis(hourAxis);

        return new ChartPanel(chart);
    }

    public static void updateHourlyProfileChart(ChartPanel panel, List<BackendClient.HourStat> current,
                                               List<BackendClient.HourStat> previous) {
        XYPlot plot = panel.getChart().getXYPlot();
        XYSeries currentSeries = new XYSeries("Últimos 30 días");
        for (BackendClient.HourStat s : current) {
            if (s.avgTemp != null) {
                currentSeries.add(s.hour, s.avgTemp);
            }
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(currentSeries);
        if (hasData(previous)) {
            XYSeries previousSeries = new XYSeries("Año anterior");
            for (BackendClient.HourStat s : previous) {
                if (s.avgTemp != null) {
                    previousSeries.add(s.hour, s.avgTemp);
                }
            }
            dataset.addSeries(previousSeries);
        }
        plot.setDataset(dataset);
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
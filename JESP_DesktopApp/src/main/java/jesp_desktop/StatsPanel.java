package jesp_desktop;

import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatsPanel extends JPanel {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BackendClient client;
    private final JButton btnActualizar;
    private final JLabel lblStatus;
    private final JLabel lblTrend;
    private final JLabel lblTrendDetail;
    private final JLabel lblCurMedia;
    private final JLabel lblCurMin;
    private final JLabel lblCurMax;
    private final JLabel lblCurHum;
    private final JLabel lblCurRecords;
    private final JLabel lblPrevMedia;
    private final JLabel lblPrevMin;
    private final JLabel lblPrevMax;
    private final JLabel lblPrevHum;
    private final JLabel lblPrevRecords;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final CardLayout cards;
    private final JPanel center;
    private final ChartPanel chartPanel;
    private boolean refreshing = false;

    public StatsPanel(BackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> comboVista = new JComboBox<>(new String[]{"Resumen", "Gráfica"});
        comboVista.addActionListener(e -> cards.show(center, comboVista.getSelectedItem().equals("Gráfica") ? "grafica" : "resumen"));
        topBar.add(new JLabel("Vista:"));
        topBar.add(comboVista);

        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refresh());
        topBar.add(btnActualizar);

        lblStatus = new JLabel(" ");
        topBar.add(lblStatus);
        add(topBar, BorderLayout.NORTH);

        JPanel resumen = new JPanel(new BorderLayout());

        JPanel trendPanel = new JPanel(new GridLayout(2, 1));
        trendPanel.setBorder(BorderFactory.createTitledBorder("Tendencia de la temperatura (últimas horas)"));
        lblTrend = new JLabel("—", javax.swing.SwingConstants.CENTER);
        lblTrend.setFont(new Font("Arial", Font.BOLD, 28));
        lblTrendDetail = new JLabel(" ", javax.swing.SwingConstants.CENTER);
        lblTrendDetail.setFont(new Font("Arial", Font.PLAIN, 14));
        trendPanel.add(lblTrend);
        trendPanel.add(lblTrendDetail);
        resumen.add(trendPanel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 2, 10, 0));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel curPanel = new JPanel(new GridLayout(6, 1));
        curPanel.setBorder(BorderFactory.createTitledBorder("Últimos 30 días"));
        lblCurMedia = valor(0.0, true);
        lblCurMin = valor(0.0, false);
        lblCurMax = valor(0.0, false);
        lblCurHum = valor(0.0, false);
        lblCurRecords = valor(0.0, false);
        curPanel.add(etiqueta("Media", lblCurMedia));
        curPanel.add(etiqueta("Mínima", lblCurMin));
        curPanel.add(etiqueta("Máxima", lblCurMax));
        curPanel.add(etiqueta("Humedad media", lblCurHum));
        curPanel.add(etiqueta("Registros", lblCurRecords));

        JPanel prevPanel = new JPanel(new GridLayout(6, 1));
        prevPanel.setBorder(BorderFactory.createTitledBorder("Año anterior (mismo periodo)"));
        lblPrevMedia = valor(0.0, true);
        lblPrevMin = valor(0.0, false);
        lblPrevMax = valor(0.0, false);
        lblPrevHum = valor(0.0, false);
        lblPrevRecords = valor(0.0, false);
        prevPanel.add(etiqueta("Media", lblPrevMedia));
        prevPanel.add(etiqueta("Mínima", lblPrevMin));
        prevPanel.add(etiqueta("Máxima", lblPrevMax));
        prevPanel.add(etiqueta("Humedad media", lblPrevHum));
        prevPanel.add(etiqueta("Registros", lblPrevRecords));

        grid.add(curPanel);
        grid.add(prevPanel);
        resumen.add(grid, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
                new Object[]{"Periodo", "Media (°C)", "Mín (°C)", "Máx (°C)", "Humedad media (%)", "Registros"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        resumen.add(new JScrollPane(table), BorderLayout.SOUTH);

        chartPanel = Charts.createHourlyProfileChart();

        center = new JPanel(cards = new CardLayout());
        center.add(resumen, "resumen");
        center.add(chartPanel, "grafica");
        add(center, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refresh();
            }
        });

        Timer autoTimer = new Timer(10000, e -> refresh());
        autoTimer.start();
        refresh();
    }

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;

        new Thread(() -> {
            try {
                LocalDateTime to = LocalDateTime.now();
                LocalDateTime from = LocalDate.now().minusDays(30).atStartOfDay();

                List<BackendClient.HourStat> current = client.getHourlyStats(from.format(ISO), to.format(ISO));
                List<BackendClient.HourStat> previous = client.getHourlyStats(
                        from.minusYears(1).format(ISO), to.minusYears(1).format(ISO));
                BackendClient.Summary currentSummary = client.getSummary(from.format(ISO), to.format(ISO));
                BackendClient.Summary previousSummary = client.getSummary(
                        from.minusYears(1).format(ISO), to.minusYears(1).format(ISO));
                BackendClient.Trend trend = client.getTrend(from.format(ISO), to.format(ISO));

                SwingUtilities.invokeLater(() -> {
                    Charts.updateHourlyProfileChart(chartPanel, current, previous);

                    mostrarTendencia(trend);
                    mostrarResumen(currentSummary, previousSummary);

                    tableModel.setRowCount(0);
                    tableModel.addRow(filaPeriodo("Últimos 30 días", currentSummary));
                    tableModel.addRow(filaPeriodo("Año anterior", previousSummary));
                    if (!currentSummary.isEmpty() && !previousSummary.isEmpty()) {
                        tableModel.addRow(filaDelta(currentSummary, previousSummary));
                    }

                    lblStatus.setText("Periodo: " + from.toLocalDate() + " → " + to.toLocalDate()
                            + "  |  Año anterior: " + from.minusYears(1).toLocalDate() + " → " + to.minusYears(1).toLocalDate());
                    lblStatus.setForeground(new Color(0, 150, 0));

                    revalidate();
                    repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Error calculando estadísticas: " + ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                });
            } finally {
                refreshing = false;
            }
        }).start();
    }

    private void mostrarTendencia(BackendClient.Trend t) {
        String flecha;
        String texto;
        Color color;
        switch (t.direction) {
            case "subiendo" -> {
                flecha = "▲";
                texto = "La temperatura está subiendo";
                color = new Color(220, 120, 0);
            }
            case "bajando" -> {
                flecha = "▼";
                texto = "La temperatura está bajando";
                color = new Color(30, 100, 220);
            }
            case "estable" -> {
                flecha = "→";
                texto = "La temperatura está estable";
                color = new Color(90, 90, 90);
            }
            default -> {
                flecha = "·";
                texto = "Sin datos suficientes";
                color = Color.GRAY;
            }
        }
        lblTrend.setText(flecha + "  " + texto);
        lblTrend.setForeground(color);
        lblTrendDetail.setText(String.format("Pendiente: %+.2f °C/h   |   Variación total: %+.2f °C   (%d muestras)",
                t.slopePerHour, t.delta, t.samples));
        lblTrendDetail.setForeground(color);
    }

    private void mostrarResumen(BackendClient.Summary cur, BackendClient.Summary prev) {
        lblCurMedia.setText(formato(cur.avgTemp) + " °C");
        lblCurMin.setText(formato(cur.minTemp) + " °C");
        lblCurMax.setText(formato(cur.maxTemp) + " °C");
        lblCurHum.setText(formato(cur.avgHum) + " %");
        lblCurRecords.setText(String.valueOf(cur.records));

        lblPrevMedia.setText(formato(prev.avgTemp) + " °C");
        lblPrevMin.setText(formato(prev.minTemp) + " °C");
        lblPrevMax.setText(formato(prev.maxTemp) + " °C");
        lblPrevHum.setText(formato(prev.avgHum) + " %");
        lblPrevRecords.setText(String.valueOf(prev.records));
    }

    private static JLabel valor(double inicial, boolean bold) {
        JLabel lbl = new JLabel("—", javax.swing.SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", bold ? Font.BOLD : Font.PLAIN, 18));
        return lbl;
    }

    private static JPanel etiqueta(String nombre, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lblNombre = new JLabel(nombre, javax.swing.SwingConstants.CENTER);
        lblNombre.setFont(new Font("Arial", Font.PLAIN, 12));
        p.add(lblNombre, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private static Object[] filaPeriodo(String nombre, BackendClient.Summary s) {
        return new Object[]{
                nombre,
                formato(s.avgTemp),
                formato(s.minTemp),
                formato(s.maxTemp),
                formato(s.avgHum),
                s.records
        };
    }

    private static Object[] filaDelta(BackendClient.Summary actual, BackendClient.Summary anterior) {
        return new Object[]{
                "Diferencia",
                delta(actual.avgTemp, anterior.avgTemp),
                delta(actual.minTemp, anterior.minTemp),
                delta(actual.maxTemp, anterior.maxTemp),
                delta(actual.avgHum, anterior.avgHum),
                actual.records - anterior.records
        };
    }

    private static String formato(Double value) {
        return value == null ? "—" : String.format("%.2f", value);
    }

    private static String delta(Double actual, Double anterior) {
        if (actual == null || anterior == null) {
            return "—";
        }
        double diff = actual - anterior;
        String signo = diff >= 0 ? "+" : "";
        return String.format("%s%.2f", signo, diff);
    }
}
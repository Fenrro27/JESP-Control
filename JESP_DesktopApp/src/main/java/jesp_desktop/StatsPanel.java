package jesp_desktop;

import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
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
        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refresh());
        topBar.add(btnActualizar);

        lblStatus = new JLabel(" ");
        topBar.add(lblStatus);
        add(topBar, BorderLayout.NORTH);

        chartPanel = Charts.createHourlyProfileChart();

        tableModel = new DefaultTableModel(
                new Object[]{"Periodo", "Media (°C)", "Mín (°C)", "Máx (°C)", "Humedad media (%)", "Registros"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        center = new JPanel(cards = new CardLayout());
        center.add(chartPanel, "chart");
        center.add(new JScrollPane(table), "list");
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

                SwingUtilities.invokeLater(() -> {
                    Charts.updateHourlyProfileChart(chartPanel, current, previous);

                    tableModel.setRowCount(0);
                    tableModel.addRow(filaPeriodo("Últimos 30 días", currentSummary));
                    tableModel.addRow(filaPeriodo("Año anterior", previousSummary));
                    if (!currentSummary.isEmpty() && !previousSummary.isEmpty()) {
                        tableModel.addRow(filaDelta(currentSummary, previousSummary));
                    }

                    lblStatus.setText("Periodo: " + from.toLocalDate() + " → " + to.toLocalDate()
                            + "  |  Año anterior: " + from.minusYears(1).toLocalDate() + " → " + to.minusYears(1).toLocalDate());
                    lblStatus.setForeground(new java.awt.Color(0, 150, 0));

                    revalidate();
                    repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Error calculando estadísticas: " + ex.getMessage());
                    lblStatus.setForeground(java.awt.Color.RED);
                });
            } finally {
                refreshing = false;
            }
        }).start();
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
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
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class HistoryPanel extends JPanel {

    private static final Integer[] LIMITS = {100, 500, 1000};
    private static final String[] MODES = {"Gráfica", "Lista"};
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BackendClient client;
    private final JComboBox<String> comboMode;
    private final JComboBox<Integer> comboLimit;
    private final JButton btnActualizar;
    private final JLabel lblStatus;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private ChartPanel chartPanel;
    private boolean refreshing = false;

    public HistoryPanel(BackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Vista:"));
        comboMode = new JComboBox<>(MODES);
        comboMode.addActionListener(e -> refresh());
        topBar.add(comboMode);

        topBar.add(new JLabel("Máx registros:"));
        comboLimit = new JComboBox<>(LIMITS);
        comboLimit.setSelectedItem(500);
        topBar.add(comboLimit);

        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refresh());
        topBar.add(btnActualizar);

        lblStatus = new JLabel(" ");
        topBar.add(lblStatus);

        add(topBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Fecha", "Hora", "Temperatura (°C)", "Humedad (%)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JPanel placeholder = new JPanel();
        placeholder.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel hint = new JLabel("Cargando historial (últimas 24 horas)...");
        placeholder.add(hint);
        add(placeholder, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refresh();
            }
        });

        Timer autoTimer = new Timer(5000, e -> refresh());
        autoTimer.start();
        refresh();
    }

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        final int limit = (Integer) comboLimit.getSelectedItem();
        final boolean listMode = "Lista".equals(comboMode.getSelectedItem());

        new Thread(() -> {
            try {
                LocalDateTime to = LocalDateTime.now();
                LocalDateTime from = to.minusHours(24);

                List<BackendClient.HistoryPoint> points =
                        client.getHistoryChartData(limit, from.format(ISO), to.format(ISO));
                Collections.sort(points, Comparator.comparingLong(p -> p.timestampMillis));

                SwingUtilities.invokeLater(() -> {
                    if (listMode) {
                        mostrarLista(points);
                    } else {
                        mostrarGrafica(points, from, to);
                    }
                    lblStatus.setText("Cargados " + points.size() + " registros (" + from.toLocalDate()
                            + " " + from.toLocalTime().withNano(0) + " → " + to.toLocalTime().withNano(0) + ")");
                    lblStatus.setForeground(new java.awt.Color(0, 150, 0));
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Error cargando historial: " + ex.getMessage());
                    lblStatus.setForeground(java.awt.Color.RED);
                });
            } finally {
                refreshing = false;
            }
        }).start();
    }

    private void mostrarGrafica(List<BackendClient.HistoryPoint> points, LocalDateTime from, LocalDateTime to) {
        if (chartPanel != null) {
            remove(chartPanel);
        }
        chartPanel = Charts.createHistoryChart(points,
                from.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                to.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        add(chartPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void mostrarLista(List<BackendClient.HistoryPoint> points) {
        if (chartPanel != null) {
            remove(chartPanel);
            chartPanel = null;
        }
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat shf = new SimpleDateFormat("HH:mm:ss");
        for (BackendClient.HistoryPoint p : points) {
            Date d = new Date(p.timestampMillis);
            tableModel.addRow(new Object[]{
                    sdf.format(d),
                    shf.format(d),
                    String.format("%.1f", p.temp),
                    String.format("%.1f", p.hum)
            });
        }
        if (table.getParent() == null) {
            add(new JScrollPane(table), BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
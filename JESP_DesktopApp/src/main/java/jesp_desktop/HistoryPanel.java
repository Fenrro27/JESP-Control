package jesp_desktop;

import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class HistoryPanel extends JPanel {

    private static final Integer[] LIMITS = {100, 500, 1000};

    private final BackendClient client;
    private final JComboBox<Integer> comboLimit;
    private final JButton btnActualizar;
    private final JLabel lblStatus;
    private ChartPanel chartPanel;

    public HistoryPanel(BackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Registros:"));
        comboLimit = new JComboBox<>(LIMITS);
        comboLimit.setSelectedItem(500);
        topBar.add(comboLimit);

        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refresh());
        topBar.add(btnActualizar);

        lblStatus = new JLabel(" ");
        topBar.add(lblStatus);

        add(topBar, BorderLayout.NORTH);

        JPanel placeholder = new JPanel();
        placeholder.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        placeholder.setLayout(new BoxLayout(placeholder, BoxLayout.Y_AXIS));
        placeholder.add(Box.createVerticalGlue());
        JLabel hint = new JLabel("Pulsa \"Actualizar\" para cargar el historial.");
        hint.setAlignmentX(CENTER_ALIGNMENT);
        placeholder.add(hint);
        placeholder.add(Box.createVerticalGlue());
        add(placeholder, BorderLayout.CENTER);
    }

    public void refresh() {
        try {
            int limit = (Integer) comboLimit.getSelectedItem();
            List<BackendClient.HistoryPoint> points = client.getHistoryChartData(limit);
            if (chartPanel != null) {
                remove(chartPanel);
            }
            chartPanel = Charts.createHistoryChart(points);
            add(chartPanel, BorderLayout.CENTER);
            lblStatus.setText("Cargados " + points.size() + " registros");
            lblStatus.setForeground(new java.awt.Color(0, 150, 0));
            revalidate();
            repaint();
        } catch (Exception ex) {
            lblStatus.setText("Error cargando historial: " + ex.getMessage());
            lblStatus.setForeground(java.awt.Color.RED);
        }
    }
}
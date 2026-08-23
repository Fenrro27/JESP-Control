package jesp_desktop;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Panel de administración de reglas contra la API JSON del backend.
 * Sustituye al antiguo editor de texto plano rules.conf.
 */
public class RulesPanel extends JPanel {

    private final BackendClient client;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblStatus;

    public RulesPanel(BackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Nombre", "Dispositivo", "Relé", "Acción",
                "Activa", "Prioridad", "Horario", "Condiciones"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(60);
        table.getColumnModel().getColumn(5).setMaxWidth(70);
        table.getColumnModel().getColumn(6).setMaxWidth(80);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnNueva = new JButton("Nueva");
        btnNueva.addActionListener(e -> abrirFormulario(null));

        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editarSeleccionada());

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminarSeleccionada());

        JButton btnRecargar = new JButton("Recargar");
        btnRecargar.addActionListener(e -> cargarReglas());

        buttons.add(btnRecargar);
        buttons.add(btnNueva);
        buttons.add(btnEditar);
        buttons.add(btnEliminar);

        lblStatus = new JLabel(" ");
        bottom.add(lblStatus, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        cargarReglas();
    }

    private void cargarReglas() {
        try {
            model.setRowCount(0);
            JSONArray rules = new JSONArray(client.getRulesJson());
            for (int i = 0; i < rules.length(); i++) {
                JSONObject r = rules.getJSONObject(i);
                model.addRow(new Object[]{
                        r.optLong("id"),
                        r.optString("name", ""),
                        r.optString("deviceId", "(defecto)"),
                        (r.optInt("relayIndex") + 1),
                        r.optBoolean("targetState") ? "ON" : "OFF",
                        r.optBoolean("enabled") ? "Sí" : "No",
                        r.optInt("priority"),
                        scheduleText(r),
                        conditionsText(r)
                });
            }
            lblStatus.setText(rules.length() + " reglas cargadas");
            lblStatus.setForeground(new java.awt.Color(0, 150, 0));
        } catch (Exception ex) {
            lblStatus.setText("Error obteniendo reglas: " + ex.getMessage());
            lblStatus.setForeground(java.awt.Color.RED);
        }
    }

    private static String scheduleText(JSONObject r) {
        String start = r.optString("timeStart", "");
        String end = r.optString("timeEnd", "");
        if (!start.isEmpty() && !end.isEmpty()) return start + " - " + end;
        return "";
    }

    private static String conditionsText(JSONObject r) {
        StringBuilder sb = new StringBuilder();
        if (r.has("tempMin") && !r.isNull("tempMin")) sb.append("T≥").append(r.getDouble("tempMin")).append(" ");
        if (r.has("tempMax") && !r.isNull("tempMax")) sb.append("T≤").append(r.getDouble("tempMax")).append(" ");
        if (r.has("humMin") && !r.isNull("humMin")) sb.append("H≥").append(r.getDouble("humMin")).append("% ");
        if (r.has("humMax") && !r.isNull("humMax")) sb.append("H≤").append(r.getDouble("humMax")).append("%");
        String logic = r.optString("conditionLogic", "AND");
        if (!logic.isEmpty()) sb.append(" [").append(logic).append("]");
        return sb.toString().trim();
    }

    private void editarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una regla de la tabla.");
            return;
        }
        long id = (long) model.getValueAt(row, 0);
        try {
            JSONArray rules = new JSONArray(client.getRulesJson());
            for (int i = 0; i < rules.length(); i++) {
                JSONObject r = rules.getJSONObject(i);
                if (r.optLong("id") == id) {
                    abrirFormulario(r);
                    return;
                }
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void eliminarSeleccionada() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una regla de la tabla.");
            return;
        }
        long id = (long) model.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this,
                "¿Eliminar la regla #" + id + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            client.deleteRule(id);
            cargarReglas();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void abrirFormulario(JSONObject existente) {
        Window owner = (Window) getTopLevelAncestor();
        RuleDialog dialog = new RuleDialog(owner, client, existente);
        dialog.setVisible(true);
        cargarReglas();
    }

    private void mostrarError(Exception ex) {
        lblStatus.setText("Error: " + ex.getMessage());
        lblStatus.setForeground(java.awt.Color.RED);
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

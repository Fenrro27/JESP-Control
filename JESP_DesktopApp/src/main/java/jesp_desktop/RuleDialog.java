package jesp_desktop;

import org.json.JSONObject;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/** Formulario para crear o editar una regla vía API JSON. */
public class RuleDialog extends JDialog {

    private final BackendClient client;
    private final JSONObject existente;

    public RuleDialog(Window owner, BackendClient client, JSONObject existente) {
        super(owner, existente == null ? "Nueva regla" : "Editar regla", ModalityType.APPLICATION_MODAL);
        this.client = client;
        this.existente = existente;

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField txtNombre = new JTextField(existente == null ? "" : existente.optString("name", ""));
        JTextField txtDispositivo = new JTextField(existente == null ? "" : existente.optString("deviceId", ""));
        JTextField txtRelay = new JTextField(String.valueOf(
                (existente == null ? 0 : existente.optInt("relayIndex")) + 1));
        JComboBox<String> cmbAccion = new JComboBox<>(new String[]{"ON", "OFF"});
        JComboBox<String> cmbLogica = new JComboBox<>(new String[]{"AND", "OR"});
        JTextField txtInicio = new JTextField(existente == null ? "" : existente.optString("timeStart", ""));
        JTextField txtFin = new JTextField(existente == null ? "" : existente.optString("timeEnd", ""));
        JTextField txtTempMin = numField(existente, "tempMin");
        JTextField txtTempMax = numField(existente, "tempMax");
        JTextField txtHumMin = numField(existente, "humMin");
        JTextField txtHumMax = numField(existente, "humMax");
        JTextField txtHisteresis = new JTextField(existente == null
                ? "0.5" : String.valueOf(existentOptFloat(existente, "hysteresis", 0.5f)));
        JTextField txtCooldown = new JTextField(String.valueOf(
                existente == null ? 0 : existente.optInt("minSwitchIntervalSeconds")));
        JTextField txtPrioridad = new JTextField(String.valueOf(
                existente == null ? 100 : existente.optInt("priority")));
        JCheckBox chkActiva = new JCheckBox("Regla activa",
                existente == null || existente.optBoolean("enabled"));

        if (existente != null && existente.optBoolean("targetState")) cmbAccion.setSelectedIndex(0);
        if (existente != null && "OR".equalsIgnoreCase(existente.optString("conditionLogic"))) {
            cmbLogica.setSelectedIndex(1);
        }

        form.add(new JLabel("Nombre:"));            form.add(txtNombre);
        form.add(new JLabel("Dispositivo (opcional):")); form.add(txtDispositivo);
        form.add(new JLabel("Relé (1-6):"));         form.add(txtRelay);
        form.add(new JLabel("Acción:"));             form.add(cmbAccion);
        form.add(new JLabel("Lógica entre condiciones:")); form.add(cmbLogica);
        form.add(new JLabel("Hora inicio (HH:mm):")); form.add(txtInicio);
        form.add(new JLabel("Hora fin (HH:mm):"));   form.add(txtFin);
        form.add(new JLabel("Temp. mínima:"));       form.add(txtTempMin);
        form.add(new JLabel("Temp. máxima:"));       form.add(txtTempMax);
        form.add(new JLabel("Humedad mínima (%):")); form.add(txtHumMin);
        form.add(new JLabel("Humedad máxima (%):")); form.add(txtHumMax);
        form.add(new JLabel("Histéresis:"));         form.add(txtHisteresis);
        form.add(new JLabel("Cooldown (s):"));       form.add(txtCooldown);
        form.add(new JLabel("Prioridad (menor=gana):")); form.add(txtPrioridad);
        form.add(chkActiva);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        buttons.add(btnCancelar);
        buttons.add(btnGuardar);
        getRootPane().setDefaultButton(btnGuardar);

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> {
            try {
                guardar(txtNombre.getText().trim(), txtDispositivo.getText().trim(),
                        Integer.parseInt(txtRelay.getText().trim()),
                        "ON".equals(cmbAccion.getSelectedItem()),
                        "OR".equals(cmbLogica.getSelectedItem()),
                        txtInicio.getText().trim(), txtFin.getText().trim(),
                        parseOrNull(txtTempMin), parseOrNull(txtTempMax),
                        parseOrNull(txtHumMin), parseOrNull(txtHumMax),
                        Float.parseFloat(txtHisteresis.getText().trim()),
                        Integer.parseInt(txtCooldown.getText().trim()),
                        Integer.parseInt(txtPrioridad.getText().trim()),
                        chkActiva.isSelected());
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Datos inválidos: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private static JTextField numField(JSONObject source, String key) {
        if (source != null && source.has(key) && !source.isNull(key)) {
            return new JTextField(String.valueOf(source.getDouble(key)));
        }
        return new JTextField();
    }

    private static float existentOptFloat(JSONObject source, String key, float fallback) {
        return source.has(key) && !source.isNull(key)
                ? (float) source.optDouble(key, fallback) : fallback;
    }

    private static Double parseOrNull(JTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' no es un número");
        }
    }

    private void guardar(String nombre, String dispositivo, int relayUnoBasado, boolean accionOn,
                         boolean orLogic, String horaInicio, String horaFin,
                         Double tempMin, Double tempMax, Double humMin, Double humMax,
                         float histeresis, int cooldown, int prioridad, boolean activa)
            throws Exception {
        JSONObject rule = new JSONObject();
        if (!nombre.isEmpty()) rule.put("name", nombre);
        if (!dispositivo.isEmpty()) rule.put("deviceId", dispositivo);
        rule.put("relayIndex", relayUnoBasado - 1);
        rule.put("targetState", accionOn);
        rule.put("conditionLogic", orLogic ? "OR" : "AND");
        if (!horaInicio.isEmpty()) rule.put("timeStart", horaInicio);
        if (!horaFin.isEmpty()) rule.put("timeEnd", horaFin);
        putIfNotNull(rule, "tempMin", tempMin);
        putIfNotNull(rule, "tempMax", tempMax);
        putIfNotNull(rule, "humMin", humMin);
        putIfNotNull(rule, "humMax", humMax);
        rule.put("hysteresis", histeresis);
        rule.put("minSwitchIntervalSeconds", cooldown);
        rule.put("priority", prioridad);
        rule.put("enabled", activa);

        if (existente == null) {
            client.createRule(rule);
        } else {
            client.updateRule(existente.getLong("id"), rule);
        }
    }

    private static void putIfNotNull(JSONObject rule, String key, Double value) {
        if (value != null) rule.put(key, value);
    }
}

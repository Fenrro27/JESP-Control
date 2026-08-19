package jesp_desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public class RulesPanel extends JPanel {

    private final BackendClient client;
    private final JTextArea textArea;
    private final JLabel lblStatus;

    public RulesPanel(BackendClient client) {
        this.client = client;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setLineWrap(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnRecargar = new JButton("Recargar");
        btnRecargar.addActionListener(e -> cargarReglas());

        JButton btnGuardar = new JButton("Guardar y Aplicar");
        btnGuardar.addActionListener(e -> guardarReglas());

        buttons.add(btnRecargar);
        buttons.add(btnGuardar);

        lblStatus = new JLabel(" ");
        bottom.add(lblStatus, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        cargarReglas();
    }

    private void cargarReglas() {
        try {
            textArea.setText(client.getRules());
            lblStatus.setText("Reglas cargadas del servidor");
            lblStatus.setForeground(new java.awt.Color(0, 150, 0));
        } catch (Exception ex) {
            lblStatus.setText("Error obteniendo reglas: " + ex.getMessage());
            lblStatus.setForeground(java.awt.Color.RED);
        }
    }

    private void guardarReglas() {
        try {
            client.saveRules(textArea.getText());
            lblStatus.setText("Reglas guardadas y aplicadas");
            lblStatus.setForeground(new java.awt.Color(0, 150, 0));
        } catch (Exception ex) {
            lblStatus.setText("Error al guardar: " + ex.getMessage());
            lblStatus.setForeground(java.awt.Color.RED);
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
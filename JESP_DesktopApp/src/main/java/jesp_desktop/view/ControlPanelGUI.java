package jesp_desktop.view;

import jesp_desktop.controller.BackendClient;
import jesp_desktop.model.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class ControlPanelGUI {
    private JFrame frame;
    private JLabel lblTemp;
    private JLabel lblHum;
    private JLabel lblConnectionStatus;
    private JToggleButton[] btnRelays;
    private BackendClient client;

    public ControlPanelGUI(BackendClient client) {
        this.client = client;
    }

    public void show() {
        frame = new JFrame("JESP Desktop App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 350);
        frame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu menuOpciones = new JMenu("Opciones");
        
        JMenuItem itemLogin = new JMenuItem("Iniciar Sesión...");
        itemLogin.addActionListener(e -> showLoginDialog());

        JMenuItem itemLogout = new JMenuItem("Cerrar Sesión");
        itemLogout.addActionListener(e -> {
            client.token = null;
            client.role = "GUEST";
            JOptionPane.showMessageDialog(frame, "Sesión cerrada. Estás en modo INVITADO.");
        });

        JMenuItem itemCambiarIp = new JMenuItem("Cambiar IP del Servidor...");
        itemCambiarIp.addActionListener(e -> {
            String currentIp = ConfigManager.getServerIp();
            String newIp = JOptionPane.showInputDialog(frame, "Introduce la nueva IP y puerto del servidor:", currentIp);
            if (newIp != null && !newIp.trim().isEmpty()) {
                client.setServerIp(newIp.trim());
                JOptionPane.showMessageDialog(frame, "IP del servidor actualizada a: " + newIp.trim());
            }
        });
        
        JMenuItem itemReglas = new JMenuItem("Editar Reglas Automáticas...");
        itemReglas.addActionListener(e -> editarReglas());
        
        JMenuItem itemHistorial = new JMenuItem("Ver Historial...");
        itemHistorial.addActionListener(e -> mostrarHistorial());
        
        menuOpciones.add(itemLogin);
        menuOpciones.add(itemLogout);
        menuOpciones.addSeparator();
        menuOpciones.add(itemCambiarIp);
        menuOpciones.add(itemReglas);
        menuOpciones.add(itemHistorial);
        menuBar.add(menuOpciones);
        frame.setJMenuBar(menuBar);

        JPanel panelSensors = new JPanel(new GridLayout(2, 1));
        panelSensors.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        lblTemp = new JLabel("Temperatura: -- °C", SwingConstants.CENTER);
        lblTemp.setFont(new Font("Arial", Font.BOLD, 24));
        lblHum = new JLabel("Humedad: -- %", SwingConstants.CENTER);
        lblHum.setFont(new Font("Arial", Font.BOLD, 24));
        panelSensors.add(lblTemp);
        panelSensors.add(lblHum);
        frame.add(panelSensors, BorderLayout.NORTH);

        JPanel panelRelays = new JPanel(new GridLayout(2, 3, 10, 10));
        panelRelays.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnRelays = new JToggleButton[6];
        
        for (int i = 0; i < 6; i++) {
            final int index = i;
            btnRelays[i] = new JToggleButton("Relé " + (i + 1));
            btnRelays[i].setFont(new Font("Arial", Font.PLAIN, 16));
            btnRelays[i].setFocusPainted(false);
            btnRelays[i].addActionListener(e -> {
                if (client.token == null) {
                    JOptionPane.showMessageDialog(frame, "Debes iniciar sesión para controlar los relés.", "No Autorizado", JOptionPane.WARNING_MESSAGE);
                    btnRelays[index].setSelected(!btnRelays[index].isSelected()); // Revert toggle
                    return;
                }
                boolean state = btnRelays[index].isSelected();
                client.setRelay(index, state); // Enviar al backend vía API
                updateButtonColor(index);
                
                String expMsg = "ADMIN".equals(client.role) ? "permanentemente" : "por 2 horas";
                System.out.println("Relé " + (index+1) + " cambiado " + expMsg);
            });
            panelRelays.add(btnRelays[i]);
            updateButtonColor(i);
        }
        frame.add(panelRelays, BorderLayout.CENTER);

        JPanel panelBottom = new JPanel(new BorderLayout());
        JPanel panelButton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnResetOverride = new JButton("Restaurar Control Automático (Reglas)");
        btnResetOverride.setFont(new Font("Arial", Font.PLAIN, 14));
        btnResetOverride.addActionListener(e -> {
            if (client.token == null) {
                JOptionPane.showMessageDialog(frame, "Debes iniciar sesión para resetear el control manual.", "No Autorizado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            client.resetOverrides(); // Enviar comando reset al backend
            JOptionPane.showMessageDialog(frame, "Control manual reseteado en el servidor.\nLas reglas definidas en el backend volverán a evaluar los relés.");
        });
        panelButton.add(btnResetOverride);
        panelBottom.add(panelButton, BorderLayout.NORTH);

        lblConnectionStatus = new JLabel("Estado: Desconectado", SwingConstants.CENTER);
        lblConnectionStatus.setFont(new Font("Arial", Font.BOLD, 14));
        lblConnectionStatus.setForeground(Color.RED);
        lblConnectionStatus.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelBottom.add(lblConnectionStatus, BorderLayout.SOUTH);

        frame.add(panelBottom, BorderLayout.SOUTH);

        // Actualizar UI cuando cambie la conexión
        client.onConnectionChange = () -> {
            SwingUtilities.invokeLater(() -> {
                if (client.isConnected) {
                    lblConnectionStatus.setText("Estado: Conectado");
                    lblConnectionStatus.setForeground(new Color(0, 150, 0)); // Verde oscuro
                } else {
                    lblConnectionStatus.setText("Estado: Desconectado");
                    lblConnectionStatus.setForeground(Color.RED);
                }
            });
        };
        // Forzar actualización inicial para evitar problemas si conectó muy rápido
        client.onConnectionChange.run();

        // Actualizar UI cuando llegue nueva información del backend
        client.onUpdate = () -> {
            SwingUtilities.invokeLater(() -> {
                lblTemp.setText(String.format("Temperatura: %.1f °C", client.temp));
                lblHum.setText(String.format("Humedad: %.1f %%", client.hum));
                for (int i = 0; i < 6; i++) {
                    // Actualizar el estado visual del botón si difiere del real
                    if (btnRelays[i].isSelected() != client.relays[i]) {
                        btnRelays[i].setSelected(client.relays[i]);
                    }
                    
                    // Mostrar si está anulado manualmente
                    if (client.overrides[i]) {
                        btnRelays[i].setText("Relé " + (i + 1) + " (Man)");
                    } else {
                        btnRelays[i].setText("Relé " + (i + 1));
                    }
                    
                    updateButtonColor(i);
                }
            });
        };

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> showLoginDialog());
    }
    
    private void updateButtonColor(int index) {
        if (btnRelays[index].isSelected()) {
            btnRelays[index].setBackground(new Color(144, 238, 144)); // Verde claro
        } else {
            btnRelays[index].setBackground(UIManager.getColor("ToggleButton.background"));
        }
    }

    private void mostrarHistorial() {
        try {
            String json = client.getHistory();
            json = json.replace("[", "").replace("]", "").trim();
            String[] records = json.isEmpty() ? new String[0] : json.split("\\},\\{");
            
            String[] columnNames = {"Fecha/Hora", "Temperatura (°C)", "Humedad (%)"};
            Object[][] data = new Object[records.length][3];
            
            for (int i = 0; i < records.length; i++) {
                String record = records[i].replace("{", "").replace("}", "");
                String[] fields = record.split(",");
                String ts = "", temp = "", hum = "";
                for (String field : fields) {
                    String[] kv = field.split(":");
                    if (kv.length >= 2) {
                        String key = kv[0].replace("\"", "").trim();
                        String val = kv[1].replace("\"", "").trim();
                        if (key.equals("timestamp")) {
                            val = field.substring(field.indexOf(":") + 1).replace("\"", "").trim();
                            ts = val;
                        }
                        if (key.equals("temp")) temp = val;
                        if (key.equals("hum")) hum = val;
                    }
                }
                data[i][0] = ts;
                data[i][1] = temp;
                data[i][2] = hum;
            }
            
            JTable table = new JTable(data, columnNames);
            JScrollPane scrollPane = new JScrollPane(table);
            
            JDialog dialog = new JDialog(frame, "Historial de Sensores (Últimos 100)", true);
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(frame);
            dialog.add(scrollPane);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error obteniendo historial: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarReglas() {
        if (!"ADMIN".equals(client.role)) {
            JOptionPane.showMessageDialog(frame, "Solo los administradores pueden editar las reglas.", "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String rulesText = client.getRules();
            JTextArea textArea = new JTextArea(rulesText);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            JDialog dialog = new JDialog(frame, "Editar Reglas Automáticas", true);
            dialog.setSize(600, 500);
            dialog.setLocationRelativeTo(frame);
            dialog.setLayout(new BorderLayout());
            
            dialog.add(scrollPane, BorderLayout.CENTER);
            
            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnGuardar = new JButton("Guardar y Aplicar");
            btnGuardar.addActionListener(ev -> {
                try {
                    client.saveRules(textArea.getText());
                    JOptionPane.showMessageDialog(dialog, "Reglas guardadas y aplicadas correctamente en el servidor.");
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(ev -> dialog.dispose());
            
            panelBotones.add(btnCancelar);
            panelBotones.add(btnGuardar);
            dialog.add(panelBotones, BorderLayout.SOUTH);
            
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error al obtener las reglas del servidor: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLoginDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Usuario:"));
        panel.add(usernameField);
        panel.add(new JLabel("Contraseña:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Iniciar Sesión", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());
            boolean success = client.login(user, pass);
            if (success) {
                JOptionPane.showMessageDialog(frame, "Sesión iniciada correctamente como: " + client.role);
            } else {
                JOptionPane.showMessageDialog(frame, "Credenciales incorrectas o error de red.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

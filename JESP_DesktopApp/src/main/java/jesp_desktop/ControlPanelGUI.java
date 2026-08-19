package jesp_desktop;

import javax.swing.*;
import java.awt.*;

public class ControlPanelGUI {
    private JFrame frame;
    private JLabel lblTemp;
    private JLabel lblHum;
    private JLabel lblConnectionStatus;
    private JToggleButton[] btnRelays;
    private BackendClient client;
    private HistoryPanel historyPanel;
    private StatsPanel statsPanel;

    public ControlPanelGUI(BackendClient client) {
        this.client = client;
    }

    public void show() {
        frame = new JFrame("JESP Desktop App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        frame.setJMenuBar(crearMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Control", crearTabControl());
        historyPanel = new HistoryPanel(client);
        tabs.addTab("Historial", historyPanel);
        tabs.addTab("Reglas", new RulesPanel(client));
        statsPanel = new StatsPanel(client);
        tabs.addTab("Estadísticas", statsPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == historyPanel) {
                historyPanel.refresh();
            } else if (tabs.getSelectedComponent() == statsPanel) {
                statsPanel.refresh();
            }
        });

        frame.add(tabs, BorderLayout.CENTER);

        // Actualizar UI cuando cambie la conexión
        client.onConnectionChange = () -> {
            SwingUtilities.invokeLater(() -> {
                if (client.isConnected) {
                    lblConnectionStatus.setText("Estado: Conectado");
                    lblConnectionStatus.setForeground(new Color(0, 150, 0));
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
                    if (btnRelays[i].isSelected() != client.relays[i]) {
                        btnRelays[i].setSelected(client.relays[i]);
                    }

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
    }

    private JMenuBar crearMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuOpciones = new JMenu("Opciones");

        JMenuItem itemCambiarIp = new JMenuItem("Cambiar IP del Servidor...");
        itemCambiarIp.addActionListener(e -> {
            String currentIp = ConfigManager.getServerIp();
            String newIp = JOptionPane.showInputDialog(frame, "Introduce la nueva IP y puerto del servidor:", currentIp);
            if (newIp != null && !newIp.trim().isEmpty()) {
                client.setServerIp(newIp.trim());
                JOptionPane.showMessageDialog(frame, "IP del servidor actualizada a: " + newIp.trim());
            }
        });

        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.addActionListener(e -> System.exit(0));

        menuOpciones.add(itemCambiarIp);
        menuOpciones.addSeparator();
        menuOpciones.add(itemSalir);
        menuBar.add(menuOpciones);
        return menuBar;
    }

    private JPanel crearTabControl() {
        JPanel tab = new JPanel(new BorderLayout());

        JPanel panelSensors = new JPanel(new GridLayout(2, 1));
        panelSensors.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        lblTemp = new JLabel("Temperatura: -- °C", SwingConstants.CENTER);
        lblTemp.setFont(new Font("Arial", Font.BOLD, 24));
        lblHum = new JLabel("Humedad: -- %", SwingConstants.CENTER);
        lblHum.setFont(new Font("Arial", Font.BOLD, 24));
        panelSensors.add(lblTemp);
        panelSensors.add(lblHum);
        tab.add(panelSensors, BorderLayout.NORTH);

        JPanel panelRelays = new JPanel(new GridLayout(2, 3, 10, 10));
        panelRelays.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnRelays = new JToggleButton[6];

        for (int i = 0; i < 6; i++) {
            final int index = i;
            btnRelays[i] = new JToggleButton("Relé " + (i + 1));
            btnRelays[i].setFont(new Font("Arial", Font.PLAIN, 16));
            btnRelays[i].setFocusPainted(false);
            btnRelays[i].addActionListener(e -> {
                boolean state = btnRelays[index].isSelected();
                client.setRelay(index, state);
                updateButtonColor(index);
            });
            panelRelays.add(btnRelays[i]);
            updateButtonColor(i);
        }
        tab.add(panelRelays, BorderLayout.CENTER);

        JPanel panelBottom = new JPanel(new BorderLayout());
        JPanel panelButton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnResetOverride = new JButton("Restaurar Control Automático (Reglas)");
        btnResetOverride.setFont(new Font("Arial", Font.PLAIN, 14));
        btnResetOverride.addActionListener(e -> {
            client.resetOverrides();
            JOptionPane.showMessageDialog(frame, "Control manual reseteado en el servidor.\nLas reglas definidas en el backend volverán a evaluar los relés.");
        });
        panelButton.add(btnResetOverride);
        panelBottom.add(panelButton, BorderLayout.NORTH);

        lblConnectionStatus = new JLabel("Estado: Desconectado", SwingConstants.CENTER);
        lblConnectionStatus.setFont(new Font("Arial", Font.BOLD, 14));
        lblConnectionStatus.setForeground(Color.RED);
        lblConnectionStatus.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelBottom.add(lblConnectionStatus, BorderLayout.SOUTH);

        tab.add(panelBottom, BorderLayout.SOUTH);
        return tab;
    }

    private void updateButtonColor(int index) {
        if (btnRelays[index].isSelected()) {
            btnRelays[index].setBackground(new Color(144, 238, 144));
        } else {
            btnRelays[index].setBackground(UIManager.getColor("ToggleButton.background"));
        }
    }
}
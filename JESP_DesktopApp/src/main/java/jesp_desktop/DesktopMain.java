package jesp_desktop;

import jesp_desktop.controller.BackendClient;
import jesp_desktop.view.ControlPanelGUI;
import jesp_desktop.model.ConfigManager;

import javax.swing.SwingUtilities;

public class DesktopMain {
    public static void main(String[] args) {
        System.out.println("Iniciando JESP Desktop App...");
        
        BackendClient client = new BackendClient();
        client.startPolling(); // Inicia la recolección de estado en segundo plano

        SwingUtilities.invokeLater(() -> {
            ControlPanelGUI gui = new ControlPanelGUI(client);
            gui.show();
        });
    }
}

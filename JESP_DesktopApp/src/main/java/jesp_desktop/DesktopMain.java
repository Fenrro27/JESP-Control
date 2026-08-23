package jesp_desktop;

import javax.swing.SwingUtilities;

public class DesktopMain {
    public static void main(String[] args) {
        System.out.println("Iniciando JESP Desktop App...");

        SwingUtilities.invokeLater(() -> {
            BackendClient client = new BackendClient();

            LoginDialog dialog = new LoginDialog(null, client);
            dialog.setVisible(true); // bloquea hasta cerrar

            if (!dialog.isSucceeded()) {
                System.out.println("Inicio de sesión cancelado.");
                System.exit(0);
            }

            client.startPolling(); // Inicia la recolección de estado en segundo plano
            ControlPanelGUI gui = new ControlPanelGUI(client);
            gui.show();
        });
    }
}

package jesp_desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/** Diálogo modal de inicio de sesión contra el backend (JWT). */
public class LoginDialog extends JDialog {

    private final BackendClient client;
    private boolean succeeded = false;

    public LoginDialog(Window owner, BackendClient client) {
        super(owner, "Iniciar sesión - JESP Control", ModalityType.APPLICATION_MODAL);
        this.client = client;

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        form.add(new JLabel("Usuario:"));
        form.add(txtUser);
        form.add(new JLabel("Contraseña:"));
        form.add(txtPass);

        JLabel lblError = new JLabel(" ");
        lblError.setForeground(java.awt.Color.RED);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLogin = new JButton("Entrar");
        JButton btnExit = new JButton("Salir");
        buttons.add(btnExit);
        buttons.add(btnLogin);

        getRootPane().setDefaultButton(btnLogin);

        btnLogin.addActionListener(e -> {
            try {
                client.login(txtUser.getText().trim(), new String(txtPass.getPassword()));
                succeeded = true;
                dispose();
            } catch (Exception ex) {
                lblError.setText(ex.getMessage());
            }
        });

        btnExit.addActionListener(e -> {
            succeeded = false;
            dispose();
        });

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(lblError, BorderLayout.NORTH);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}

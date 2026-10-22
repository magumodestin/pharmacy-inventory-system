package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.DatabaseSetup;
import com.healthfirst.pims.db.UserDAO;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

/**
 * Entry point screen: authenticates a user and redirects based on role.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel statusLabel = new JLabel(" ");
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("HealthFirst PIMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(240, 245, 250));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("HealthFirst Pharmacy", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        root.add(title, gc);

        JLabel subtitle = new JLabel("Inventory Management System", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gc.gridy = 1;
        root.add(subtitle, gc);

        gc.gridwidth = 1;
        gc.gridy = 2; gc.gridx = 0;
        root.add(new JLabel("Username:"), gc);
        gc.gridx = 1;
        root.add(usernameField, gc);

        gc.gridy = 3; gc.gridx = 0;
        root.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        root.add(passwordField, gc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(30, 90, 160));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.addActionListener(this::handleLogin);
        gc.gridy = 4; gc.gridx = 0; gc.gridwidth = 2;
        root.add(loginBtn, gc);

        statusLabel.setForeground(Color.RED);
        gc.gridy = 5;
        root.add(statusLabel, gc);

        // Enter key triggers login from the password field
        passwordField.addActionListener(this::handleLogin);

        add(root);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        User user = userDAO.login(username, password);
        if (user == null) {
            statusLabel.setText("Invalid username or password.");
            passwordField.setText("");
            return;
        }

        dispose();
        if (user.isAdmin()) {
            new AdminDashboard(user).setVisible(true);
        } else {
            new CashierDashboard(user).setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (!prepareDatabase()) {
                System.exit(0);
            }
            new LoginFrame().setVisible(true);
        });
    }

    /**
     * Checks that MySQL is running and creates pims_db from the bundled script on first run.
     * If MySQL is not reachable the user can start it and press Retry.
     *
     * @return false if the user chose to exit
     */
    private static boolean prepareDatabase() {
        while (true) {
            try {
                DatabaseSetup.ensureReady();
                return true;
            } catch (SQLException e) {
                Object[] options = {"Retry", "Exit"};
                int choice = JOptionPane.showOptionDialog(null, e.getMessage(), "Database problem",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
                if (choice != 0) {
                    return false;
                }
            }
        }
    }
}

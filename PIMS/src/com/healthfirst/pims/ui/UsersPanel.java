package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.UserDAO;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin tab for creating, updating and deleting Cashier accounts.
 * When updating, leave the password blank to keep the current one.
 */
public class UsersPanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);

    private final UserDAO userDAO = new UserDAO();

    private final String[] columns = {"ID", "Full Name", "Username", "Role"};
    private final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<User> current = new ArrayList<>(); // same order as table rows

    private final JTextField fullNameField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);

    private int selectedId = -1; // -1 = nothing selected

    public UsersPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildTable(), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        loadUsers();
    }

    // ------------------------------------------------------------------ UI

    private JScrollPane buildTable() {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < current.size()) {
                fillForm(current.get(row));
            }
        });

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        return new JScrollPane(table);
    }

    private JPanel buildFormPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addField(fields, "Full name:", fullNameField, 0, 0);
        addField(fields, "Username:", usernameField, 1, 0);
        addField(fields, "Password:", passwordField, 2, 0);

        JLabel hint = new JLabel("Cashier accounts only. When updating, leave Password blank to keep the current one.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 6;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 6, 4, 6);
        fields.add(hint, gc);

        JButton addBtn = styledButton("Add");
        JButton updateBtn = styledButton("Update");
        JButton deleteBtn = styledButton("Delete");
        JButton clearBtn = styledButton("Clear");
        deleteBtn.setBackground(new Color(190, 50, 50));

        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        clearBtn.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);

        JPanel form = new JPanel(new BorderLayout());
        form.setBorder(new TitledBorder("Cashier Account"));
        form.add(fields, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }

    private void addField(JPanel panel, String label, JComponent field, int col, int row) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = col * 2;
        gc.gridy = row;
        panel.add(new JLabel(label), gc);

        gc.gridx = col * 2 + 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        panel.add(field, gc);
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    // ---------------------------------------------------------------- data

    private void loadUsers() {
        try {
            current = userDAO.getCashiers();
            tableModel.setRowCount(0);
            for (User u : current) {
                tableModel.addRow(new Object[]{
                    u.getUserId(), u.getFullName(), u.getUsername(), u.getRole()
                });
            }
            selectedId = -1;
        } catch (SQLException e) {
            showError("Could not load users", e);
        }
    }

    // ------------------------------------------------------------- actions

    private void onAdd() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (!validateNames(fullName, username)) {
            return;
        }
        if (password.length() < 4) {
            warn("Password must be at least 4 characters.");
            return;
        }

        try {
            userDAO.addUser(new User(0, username, password, "Cashier", fullName));
            loadUsers();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cashier account created.");
        } catch (SQLException e) {
            handleSaveError("Could not create account", e);
        }
    }

    private void onUpdate() {
        if (selectedId < 0) {
            warn("Select a cashier in the table first.");
            return;
        }
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (!validateNames(fullName, username)) {
            return;
        }
        boolean changePassword = !password.isEmpty();
        if (changePassword && password.length() < 4) {
            warn("Password must be at least 4 characters (or leave it blank to keep the current one).");
            return;
        }

        try {
            userDAO.updateUser(new User(selectedId, username, password, "Cashier", fullName), changePassword);
            loadUsers();
            clearForm();
            JOptionPane.showMessageDialog(this, "Cashier account updated.");
        } catch (SQLException e) {
            handleSaveError("Could not update account", e);
        }
    }

    private void onDelete() {
        if (selectedId < 0) {
            warn("Select a cashier in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the selected cashier account?\n"
              + "Their past sales will be kept in the reports.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userDAO.deleteUser(selectedId);
            loadUsers();
            clearForm();
        } catch (SQLException e) {
            showError("Could not delete account", e);
        }
    }

    // ---------------------------------------------------------------- form

    private void fillForm(User u) {
        selectedId = u.getUserId();
        fullNameField.setText(u.getFullName());
        usernameField.setText(u.getUsername());
        passwordField.setText(""); // never show the stored password
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        fullNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        fullNameField.requestFocusInWindow();
    }

    private boolean validateNames(String fullName, String username) {
        if (fullName.isEmpty()) {
            warn("Full name is required.");
            return false;
        }
        if (fullName.length() > 100) {
            warn("Full name can be at most 100 characters.");
            return false;
        }
        if (username.length() < 3 || username.length() > 50 || username.contains(" ")) {
            warn("Username must be 3-50 characters with no spaces.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------- dialogs

    private void handleSaveError(String title, SQLException e) {
        if (e.getErrorCode() == 1062) { // duplicate entry on the UNIQUE username column
            warn("That username is already taken. Please choose another.");
        } else {
            showError(title, e);
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String title, SQLException e) {
        JOptionPane.showMessageDialog(this, title + ":\n" + e.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }
}

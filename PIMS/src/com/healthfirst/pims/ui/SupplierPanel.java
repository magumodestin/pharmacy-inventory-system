package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.SupplierDAO;
import com.healthfirst.pims.model.Supplier;

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
 * Admin tab for full CRUD on suppliers: table at the top, form at the bottom.
 */
public class SupplierPanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);

    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final String[] columns = {"ID", "Name", "Contact Person", "Phone", "Email", "Address"};
    private final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<Supplier> current = new ArrayList<>(); // same order as table rows

    private final JTextField nameField = new JTextField(14);
    private final JTextField contactField = new JTextField(14);
    private final JTextField phoneField = new JTextField(14);
    private final JTextField emailField = new JTextField(14);
    private final JTextField addressField = new JTextField(14);

    private int selectedId = -1; // -1 = nothing selected

    public SupplierPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildTable(), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        loadSuppliers();
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
        addField(fields, "Name:", nameField, 0, 0);
        addField(fields, "Contact person:", contactField, 1, 0);
        addField(fields, "Phone:", phoneField, 2, 0);
        addField(fields, "Email:", emailField, 0, 1);
        addField(fields, "Address:", addressField, 1, 1);

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
        form.setBorder(new TitledBorder("Supplier Details"));
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

    private void loadSuppliers() {
        try {
            current = supplierDAO.getAll();
            tableModel.setRowCount(0);
            for (Supplier s : current) {
                tableModel.addRow(new Object[]{
                    s.getSupplierId(),
                    s.getName(),
                    nullToEmpty(s.getContactPerson()),
                    nullToEmpty(s.getPhone()),
                    nullToEmpty(s.getEmail()),
                    nullToEmpty(s.getAddress())
                });
            }
            selectedId = -1;
        } catch (SQLException e) {
            showError("Could not load suppliers", e);
        }
    }

    // ------------------------------------------------------------- actions

    private void onAdd() {
        Supplier s = readForm();
        if (s == null) {
            return;
        }
        try {
            supplierDAO.add(s);
            loadSuppliers();
            clearForm();
            JOptionPane.showMessageDialog(this, "Supplier added.");
        } catch (SQLException e) {
            showError("Could not add supplier", e);
        }
    }

    private void onUpdate() {
        if (selectedId < 0) {
            warn("Select a supplier in the table first.");
            return;
        }
        Supplier s = readForm();
        if (s == null) {
            return;
        }
        s.setSupplierId(selectedId);
        try {
            supplierDAO.update(s);
            loadSuppliers();
            clearForm();
            JOptionPane.showMessageDialog(this, "Supplier updated.");
        } catch (SQLException e) {
            showError("Could not update supplier", e);
        }
    }

    private void onDelete() {
        if (selectedId < 0) {
            warn("Select a supplier in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the selected supplier?\n"
              + "Medicines from this supplier will remain, but show no supplier.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            supplierDAO.delete(selectedId);
            loadSuppliers();
            clearForm();
        } catch (SQLException e) {
            showError("Could not delete supplier", e);
        }
    }

    // ---------------------------------------------------------------- form

    private void fillForm(Supplier s) {
        selectedId = s.getSupplierId();
        nameField.setText(nullToEmpty(s.getName()));
        contactField.setText(nullToEmpty(s.getContactPerson()));
        phoneField.setText(nullToEmpty(s.getPhone()));
        emailField.setText(nullToEmpty(s.getEmail()));
        addressField.setText(nullToEmpty(s.getAddress()));
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        nameField.setText("");
        contactField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        nameField.requestFocusInWindow();
    }

    /** Validates the form. Returns a Supplier, or null (after showing a message) if invalid. */
    private Supplier readForm() {
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty()) {
            warn("Supplier name is required.");
            return null;
        }
        if (name.length() > 100 || contact.length() > 100 || email.length() > 100) {
            warn("Name, contact person and email can be at most 100 characters each.");
            return null;
        }
        if (!phone.isEmpty() && !phone.matches("[0-9+()\\- ]{7,20}")) {
            warn("Phone must be 7-20 characters using digits, spaces, + ( ) or -, e.g. 011-555-0101");
            return null;
        }
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            warn("Email does not look valid, e.g. name@company.co.za");
            return null;
        }

        Supplier s = new Supplier();
        s.setName(name);
        s.setContactPerson(contact);
        s.setPhone(phone);
        s.setEmail(email);
        s.setAddress(address);
        return s;
    }

    // ------------------------------------------------------------- helpers

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String title, SQLException e) {
        JOptionPane.showMessageDialog(this, title + ":\n" + e.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }
}

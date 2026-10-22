package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.MedicineDAO;
import com.healthfirst.pims.db.SupplierDAO;
import com.healthfirst.pims.model.Medicine;
import com.healthfirst.pims.model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin tab for full CRUD on medicines: searchable table at the top,
 * add / update / delete form at the bottom.
 * Rows are highlighted: red = low stock, orange = expired.
 */
public class MedicinePanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);
    private static final Color LOW_STOCK_COLOR = new Color(255, 205, 210);
    private static final Color EXPIRED_COLOR = new Color(255, 224, 178);

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final String[] columns = {
        "ID", "Name", "Company", "Type", "Price", "In Stock", "Reorder Lvl", "Expiry", "Supplier"
    };
    private final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<Medicine> current = new ArrayList<>(); // same order as table rows

    private final JTextField searchField = new JTextField(20);

    private final JTextField nameField = new JTextField(12);
    private final JTextField companyField = new JTextField(12);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{
        "Tablet", "Capsule", "Syrup", "Injection", "Cream", "Ointment", "Drops", "Other"
    });
    private final JComboBox<Supplier> supplierBox = new JComboBox<>();
    private final JTextField priceField = new JTextField(12);
    private final JTextField qtyField = new JTextField(12);
    private final JTextField reorderField = new JTextField(12);
    private final JTextField expiryField = new JTextField(12);

    private int selectedId = -1; // -1 = nothing selected

    public MedicinePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        loadSuppliers();
        loadMedicines(null);
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JLabel("Search:"));
        left.add(searchField);
        JButton searchBtn = styledButton("Search");
        JButton showAllBtn = styledButton("Show All");
        left.add(searchBtn);
        left.add(showAllBtn);

        searchBtn.addActionListener(e -> loadMedicines(searchField.getText()));
        searchField.addActionListener(e -> loadMedicines(searchField.getText()));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            loadSuppliers();
            loadMedicines(null);
        });

        JLabel legend = new JLabel("Red = low stock    Orange = expired");
        legend.setFont(new Font("SansSerif", Font.ITALIC, 12));

        bar.add(left, BorderLayout.WEST);
        bar.add(legend, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);

        // Colour rows: expired -> orange, low stock -> red
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    if (row < current.size()) {
                        Medicine m = current.get(row);
                        if (m.getExpiryDate() != null && m.getExpiryDate().isBefore(LocalDate.now())) {
                            c.setBackground(EXPIRED_COLOR);
                        } else if (m.isLowStock()) {
                            c.setBackground(LOW_STOCK_COLOR);
                        }
                    }
                }
                return c;
            }
        });

        // Clicking a row fills the form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < current.size()) {
                fillForm(current.get(row));
            }
        });

        // Narrow ID column
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        return new JScrollPane(table);
    }

    private JPanel buildFormPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addField(fields, "Name:", nameField, 0, 0);
        addField(fields, "Company:", companyField, 1, 0);
        addField(fields, "Type:", typeBox, 2, 0);
        addField(fields, "Supplier:", supplierBox, 3, 0);
        addField(fields, "Price:", priceField, 0, 1);
        addField(fields, "Quantity:", qtyField, 1, 1);
        addField(fields, "Reorder level:", reorderField, 2, 1);
        addField(fields, "Expiry (yyyy-MM-dd):", expiryField, 3, 1);

        typeBox.setEditable(true);

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
        form.setBorder(new TitledBorder("Medicine Details"));
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
        supplierBox.removeAllItems();
        supplierBox.addItem(new Supplier(0, "-- None --", null, null, null, null));
        try {
            for (Supplier s : supplierDAO.getAll()) {
                supplierBox.addItem(s);
            }
        } catch (SQLException e) {
            showError("Could not load suppliers", e);
        }
    }

    private void loadMedicines(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                current = medicineDAO.getAll();
            } else {
                current = medicineDAO.search(keyword.trim());
            }
            tableModel.setRowCount(0);
            for (Medicine m : current) {
                tableModel.addRow(new Object[]{
                    m.getMedicineId(),
                    m.getName(),
                    m.getCompany(),
                    m.getMedicineType(),
                    m.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    m.getQuantityInStock(),
                    m.getReorderLevel(),
                    m.getExpiryDate() == null ? "" : m.getExpiryDate().toString(),
                    m.getSupplierName() == null ? "" : m.getSupplierName()
                });
            }
            selectedId = -1;
        } catch (SQLException e) {
            showError("Could not load medicines", e);
        }
    }

    /** Reloads the supplier dropdown and the table (called when the tab is re-opened). */
    public void refresh() {
        loadSuppliers();
        loadMedicines(searchField.getText());
    }

    // ------------------------------------------------------------- actions

    private void onAdd() {
        Medicine m = readForm();
        if (m == null) {
            return;
        }
        try {
            medicineDAO.add(m);
            loadMedicines(searchField.getText());
            clearForm();
            JOptionPane.showMessageDialog(this, "Medicine added.");
        } catch (SQLException e) {
            showError("Could not add medicine", e);
        }
    }

    private void onUpdate() {
        if (selectedId < 0) {
            warn("Select a medicine in the table first.");
            return;
        }
        Medicine m = readForm();
        if (m == null) {
            return;
        }
        m.setMedicineId(selectedId);
        try {
            medicineDAO.update(m);
            loadMedicines(searchField.getText());
            clearForm();
            JOptionPane.showMessageDialog(this, "Medicine updated.");
        } catch (SQLException e) {
            showError("Could not update medicine", e);
        }
    }

    private void onDelete() {
        if (selectedId < 0) {
            warn("Select a medicine in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the selected medicine? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            medicineDAO.delete(selectedId);
            loadMedicines(searchField.getText());
            clearForm();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) { // foreign key: medicine appears in sale_items
                warn("This medicine appears in past sales and cannot be deleted.\n"
                   + "Set its quantity to 0 instead.");
            } else {
                showError("Could not delete medicine", e);
            }
        }
    }

    // ---------------------------------------------------------------- form

    private void fillForm(Medicine m) {
        selectedId = m.getMedicineId();
        nameField.setText(m.getName());
        companyField.setText(m.getCompany() == null ? "" : m.getCompany());
        typeBox.setSelectedItem(m.getMedicineType());
        priceField.setText(m.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
        qtyField.setText(String.valueOf(m.getQuantityInStock()));
        reorderField.setText(String.valueOf(m.getReorderLevel()));
        expiryField.setText(m.getExpiryDate() == null ? "" : m.getExpiryDate().toString());

        supplierBox.setSelectedIndex(0); // "-- None --"
        for (int i = 0; i < supplierBox.getItemCount(); i++) {
            if (supplierBox.getItemAt(i).getSupplierId() == m.getSupplierId()) {
                supplierBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        nameField.setText("");
        companyField.setText("");
        typeBox.setSelectedIndex(0);
        supplierBox.setSelectedIndex(0);
        priceField.setText("");
        qtyField.setText("");
        reorderField.setText("");
        expiryField.setText("");
        nameField.requestFocusInWindow();
    }

    /** Validates the form. Returns a Medicine, or null (after showing a message) if invalid. */
    private Medicine readForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            warn("Medicine name is required.");
            return null;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            if (price.signum() < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            warn("Price must be a valid number of 0 or more, e.g. 25.50");
            return null;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            warn("Quantity must be a whole number of 0 or more.");
            return null;
        }

        int reorder;
        try {
            reorder = Integer.parseInt(reorderField.getText().trim());
            if (reorder < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            warn("Reorder level must be a whole number of 0 or more.");
            return null;
        }

        LocalDate expiry;
        try {
            expiry = LocalDate.parse(expiryField.getText().trim());
        } catch (DateTimeParseException ex) {
            warn("Expiry date must be in the format yyyy-MM-dd, e.g. 2027-06-30");
            return null;
        }

        Medicine m = new Medicine();
        m.setName(name);
        m.setCompany(companyField.getText().trim());
        Object type = typeBox.getSelectedItem();
        m.setMedicineType(type == null ? "Other" : type.toString().trim());
        m.setPrice(price);
        m.setQuantityInStock(qty);
        m.setReorderLevel(reorder);
        m.setExpiryDate(expiry);
        Supplier s = (Supplier) supplierBox.getSelectedItem();
        m.setSupplierId(s == null ? 0 : s.getSupplierId());
        return m;
    }

    // ------------------------------------------------------------- dialogs

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String title, SQLException e) {
        JOptionPane.showMessageDialog(this, title + ":\n" + e.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }
}

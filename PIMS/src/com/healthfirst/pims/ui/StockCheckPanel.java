package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.MedicineDAO;
import com.healthfirst.pims.model.Medicine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only lookup for cashiers: check a medicine's price and availability
 * without making a sale. There are no add / edit / delete controls here.
 */
public class StockCheckPanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);
    private static final Color LOW_STOCK_COLOR = new Color(255, 205, 210);
    private static final Color EXPIRED_COLOR = new Color(255, 224, 178);
    private static final Color OUT_OF_STOCK_COLOR = new Color(224, 224, 224);

    private final MedicineDAO medicineDAO = new MedicineDAO();

    private final String[] columns = {"Name", "Company", "Type", "Price", "In Stock", "Expiry", "Availability"};
    private final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<Medicine> current = new ArrayList<>(); // same order as table rows

    private final JTextField searchField = new JTextField(20);

    public StockCheckPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        loadMedicines(null);
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JLabel("Search medicine:"));
        left.add(searchField);
        JButton searchBtn = styledButton("Search");
        JButton showAllBtn = styledButton("Show All");
        left.add(searchBtn);
        left.add(showAllBtn);

        searchBtn.addActionListener(e -> loadMedicines(searchField.getText()));
        searchField.addActionListener(e -> loadMedicines(searchField.getText()));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            loadMedicines(null);
        });

        JLabel legend = new JLabel("Red = low stock    Orange = expired    Grey = out of stock");
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

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    if (row < current.size()) {
                        Medicine m = current.get(row);
                        if (isExpired(m)) {
                            c.setBackground(EXPIRED_COLOR);
                        } else if (m.getQuantityInStock() <= 0) {
                            c.setBackground(OUT_OF_STOCK_COLOR);
                        } else if (m.isLowStock()) {
                            c.setBackground(LOW_STOCK_COLOR);
                        }
                    }
                }
                return c;
            }
        });

        return new JScrollPane(table);
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    // ---------------------------------------------------------------- data

    /** Reloads the list using the current search text (called when the tab is re-opened). */
    public void refresh() {
        loadMedicines(searchField.getText());
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
                    m.getName(),
                    m.getCompany() == null ? "" : m.getCompany(),
                    m.getMedicineType() == null ? "" : m.getMedicineType(),
                    m.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    m.getQuantityInStock(),
                    m.getExpiryDate() == null ? "" : m.getExpiryDate().toString(),
                    availability(m)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load medicines:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------- helpers

    private String availability(Medicine m) {
        if (isExpired(m)) {
            return "Expired";
        }
        if (m.getQuantityInStock() <= 0) {
            return "Out of stock";
        }
        if (m.isLowStock()) {
            return "Low stock";
        }
        return "Available";
    }

    private boolean isExpired(Medicine m) {
        return m.getExpiryDate() != null && m.getExpiryDate().isBefore(LocalDate.now());
    }
}

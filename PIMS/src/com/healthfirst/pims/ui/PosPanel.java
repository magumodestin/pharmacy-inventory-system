package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.MedicineDAO;
import com.healthfirst.pims.db.SaleDAO;
import com.healthfirst.pims.model.Medicine;
import com.healthfirst.pims.model.Sale;
import com.healthfirst.pims.model.SaleItem;
import com.healthfirst.pims.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Point of Sale screen for cashiers (simulation only, no real payment).
 * Search a medicine, add it to the cart, then check out to save the sale,
 * reduce stock and show the bill.
 */
public class PosPanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);
    private static final Color GREEN = new Color(34, 139, 34);
    private static final Color EXPIRED_COLOR = new Color(255, 224, 178);
    private static final Color OUT_OF_STOCK_COLOR = new Color(224, 224, 224);

    private final User currentUser;
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    // search results
    private final String[] resultColumns = {"ID", "Name", "Company", "Type", "Price", "In Stock", "Expiry"};
    private final DefaultTableModel resultsModel = new DefaultTableModel(resultColumns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultsTable = new JTable(resultsModel);
    private List<Medicine> results = new ArrayList<>(); // same order as table rows

    // cart
    private final String[] cartColumns = {"Medicine", "Price", "Qty", "Line Total"};
    private final DefaultTableModel cartModel = new DefaultTableModel(cartColumns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable cartTable = new JTable(cartModel);
    private final List<SaleItem> cart = new ArrayList<>();

    private final JTextField searchField = new JTextField(20);
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JLabel totalLabel = new JLabel("Total: 0.00");

    public PosPanel(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildSearchBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildResultsPanel(), buildCartPanel());
        split.setResizeWeight(0.45);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        add(buildCheckoutBar(), BorderLayout.SOUTH);

        loadResults(null);
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.add(new JLabel("Search medicine:"));
        bar.add(searchField);
        JButton searchBtn = styledButton("Search");
        JButton showAllBtn = styledButton("Show All");
        bar.add(searchBtn);
        bar.add(showAllBtn);

        searchBtn.addActionListener(e -> loadResults(searchField.getText()));
        searchField.addActionListener(e -> loadResults(searchField.getText()));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            loadResults(null);
        });
        return bar;
    }

    private JPanel buildResultsPanel() {
        styleTable(resultsTable);
        resultsTable.getColumnModel().getColumn(0).setMaxWidth(50);

        // Orange = expired, grey = out of stock
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    if (row < results.size()) {
                        Medicine m = results.get(row);
                        if (isExpired(m)) {
                            c.setBackground(EXPIRED_COLOR);
                        } else if (m.getQuantityInStock() <= 0) {
                            c.setBackground(OUT_OF_STOCK_COLOR);
                        }
                    }
                }
                return c;
            }
        });

        // Double-click a row = add 1 to the cart
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onAddToCart();
                }
            }
        });

        JButton addBtn = styledButton("Add to Cart");
        addBtn.addActionListener(e -> onAddToCart());

        JLabel legend = new JLabel("Orange = expired    Grey = out of stock    (double-click a row to add it)");
        legend.setFont(new Font("SansSerif", Font.ITALIC, 12));

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addRow.add(new JLabel("Quantity:"));
        addRow.add(qtySpinner);
        addRow.add(addBtn);
        addRow.add(legend);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new TitledBorder("Medicines"));
        panel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        panel.add(addRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCartPanel() {
        styleTable(cartTable);

        JButton removeBtn = styledButton("Remove Selected");
        JButton clearBtn = styledButton("Clear Cart");
        clearBtn.setBackground(new Color(190, 50, 50));
        removeBtn.addActionListener(e -> onRemoveSelected());
        clearBtn.addActionListener(e -> onClearCart());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(removeBtn);
        buttons.add(clearBtn);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new TitledBorder("Cart"));
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCheckoutBar() {
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        JButton checkoutBtn = styledButton("Checkout");
        checkoutBtn.setBackground(GREEN);
        checkoutBtn.setFont(checkoutBtn.getFont().deriveFont(Font.BOLD, 16f));
        checkoutBtn.setPreferredSize(new Dimension(180, 42));
        checkoutBtn.addActionListener(e -> onCheckout());

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(4, 6, 0, 6));
        bar.add(totalLabel, BorderLayout.WEST);
        bar.add(checkoutBtn, BorderLayout.EAST);
        return bar;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));
        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    // ---------------------------------------------------------------- data

    /** Reloads the medicine list using the current search text (stock may have changed). */
    public void refreshResults() {
        loadResults(searchField.getText());
    }

    private void loadResults(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                results = medicineDAO.getAll();
            } else {
                results = medicineDAO.search(keyword.trim());
            }
            resultsModel.setRowCount(0);
            for (Medicine m : results) {
                resultsModel.addRow(new Object[]{
                    m.getMedicineId(),
                    m.getName(),
                    m.getCompany() == null ? "" : m.getCompany(),
                    m.getMedicineType() == null ? "" : m.getMedicineType(),
                    m.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    m.getQuantityInStock(),
                    m.getExpiryDate() == null ? "" : m.getExpiryDate().toString()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load medicines:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem item : cart) {
            cartModel.addRow(new Object[]{
                item.getMedicineName(),
                item.getPriceAtSale().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                item.getQuantitySold(),
                item.getLineTotal().setScale(2, RoundingMode.HALF_UP).toPlainString()
            });
            total = total.add(item.getLineTotal());
        }
        totalLabel.setText("Total: " + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    // ------------------------------------------------------------- actions

    private void onAddToCart() {
        int row = resultsTable.getSelectedRow();
        if (row < 0 || row >= results.size()) {
            warn("Select a medicine from the list first.");
            return;
        }
        Medicine m = results.get(row);
        int qty = (Integer) qtySpinner.getValue();

        if (isExpired(m)) {
            warn(m.getName() + " has expired and cannot be sold.");
            return;
        }

        SaleItem existing = findInCart(m.getMedicineId());
        int alreadyInCart = existing == null ? 0 : existing.getQuantitySold();
        if (alreadyInCart + qty > m.getQuantityInStock()) {
            warn("Not enough stock for " + m.getName() + ".\n"
               + "In stock: " + m.getQuantityInStock() + ", already in cart: " + alreadyInCart);
            return;
        }

        if (existing == null) {
            cart.add(new SaleItem(m.getMedicineId(), m.getName(), qty, m.getPrice()));
        } else {
            existing.setQuantitySold(alreadyInCart + qty);
        }
        refreshCart();
        qtySpinner.setValue(1);
    }

    private void onRemoveSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= cart.size()) {
            warn("Select an item in the cart first.");
            return;
        }
        cart.remove(row);
        refreshCart();
    }

    private void onClearCart() {
        if (cart.isEmpty()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Remove all items from the cart?",
                "Clear cart", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            cart.clear();
            refreshCart();
        }
    }

    private void onCheckout() {
        if (cart.isEmpty()) {
            warn("The cart is empty. Add at least one medicine first.");
            return;
        }
        try {
            Sale sale = saleDAO.saveSale(currentUser.getUserId(), cart);
            sale.setCashierName(currentUser.getFullName());

            List<SaleItem> billItems = new ArrayList<>(cart); // keep a copy for the bill
            cart.clear();
            refreshCart();
            loadResults(searchField.getText()); // stock has changed

            new BillDialog(SwingUtilities.getWindowAncestor(this), sale, billItems).setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Checkout failed", JOptionPane.ERROR_MESSAGE);
            loadResults(searchField.getText()); // stock may have changed since it was loaded
        }
    }

    // ------------------------------------------------------------- helpers

    private SaleItem findInCart(int medicineId) {
        for (SaleItem item : cart) {
            if (item.getMedicineId() == medicineId) {
                return item;
            }
        }
        return null;
    }

    private boolean isExpired(Medicine m) {
        return m.getExpiryDate() != null && m.getExpiryDate().isBefore(LocalDate.now());
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }
}

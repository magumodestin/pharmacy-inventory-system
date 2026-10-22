package com.healthfirst.pims.ui;

import com.healthfirst.pims.db.ReportDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin "Reports" tab with four reports:
 * Sales, Item-Wise, Low Stock and Expiry (medicines expiring within the next month).
 * Each report reloads from the database whenever it is opened or Generate is clicked.
 */
public class ReportsPanel extends JPanel {

    private static final Color PRIMARY = new Color(30, 90, 160);
    private static final Color PINK = new Color(255, 205, 210);
    private static final Color DARK_PINK = new Color(255, 171, 171);
    private static final Color ORANGE = new Color(255, 224, 178);
    private static final Color GREY = new Color(235, 235, 235);

    private final ReportDAO reportDAO = new ReportDAO();
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
    private final List<ReportView> views = new ArrayList<>();

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        // 1. Sales report
        addReport("Sales Report", new ReportView(
                new String[]{"Sale ID", "Date", "Cashier", "Items Sold", "Total"},
                true, null,
                (from, to, flag) -> reportDAO.salesReport(from, to),
                rows -> {
                    int items = 0;
                    BigDecimal total = BigDecimal.ZERO;
                    for (Object[] r : rows) {
                        items += ((Number) r[3]).intValue();
                        total = total.add((BigDecimal) r[4]);
                    }
                    return "Sales: " + rows.size() + "        Items sold: " + items
                            + "        Total revenue: " + total.toPlainString();
                },
                null));

        // 2. Item-wise report
        addReport("Item-Wise Report", new ReportView(
                new String[]{"ID", "Medicine", "Type", "Units Sold", "Revenue"},
                true, null,
                (from, to, flag) -> reportDAO.itemWiseReport(from, to),
                rows -> {
                    int units = 0;
                    int unsold = 0;
                    BigDecimal revenue = BigDecimal.ZERO;
                    for (Object[] r : rows) {
                        int u = ((Number) r[3]).intValue();
                        units += u;
                        if (u == 0) {
                            unsold++;
                        }
                        revenue = revenue.add((BigDecimal) r[4]);
                    }
                    return "Units sold: " + units + "        Revenue: " + revenue.toPlainString()
                            + "        Medicines with no sales: " + unsold;
                },
                row -> ((Number) row[3]).intValue() == 0 ? GREY : null));

        // 3. Low stock report
        addReport("Low Stock Report", new ReportView(
                new String[]{"ID", "Medicine", "Type", "In Stock", "Reorder Level", "Shortfall", "Supplier"},
                false, null,
                (from, to, flag) -> reportDAO.lowStockReport(),
                rows -> rows.size() + " medicine(s) at or below their reorder level",
                row -> ((Number) row[3]).intValue() == 0 ? DARK_PINK : PINK));

        // 4. Expiry report
        addReport("Expiry Report", new ReportView(
                new String[]{"ID", "Medicine", "Type", "In Stock", "Expiry Date", "Days Left", "Status", "Supplier"},
                false, "Include already expired",
                (from, to, flag) -> reportDAO.expiryReport(flag),
                rows -> {
                    int expired = 0;
                    for (Object[] r : rows) {
                        if (((Number) r[5]).intValue() < 0) {
                            expired++;
                        }
                    }
                    String text = rows.size() + " medicine(s) expiring within the next month";
                    return expired > 0 ? text + "  (" + expired + " already expired)" : text;
                },
                row -> {
                    int days = ((Number) row[5]).intValue();
                    if (days < 0) {
                        return ORANGE;
                    }
                    return days <= 7 ? PINK : null;
                }));

        // Reload a report each time its sub-tab is opened
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);
    }

    /** Reloads the report that is currently showing (called when the Reports tab is opened). */
    public void refresh() {
        int index = tabs.getSelectedIndex();
        if (index >= 0 && index < views.size()) {
            views.get(index).generate();
        }
    }

    private void addReport(String title, ReportView view) {
        views.add(view);
        tabs.addTab(title, view);
    }

    // ------------------------------------------------------- small interfaces

    private interface Loader {
        List<Object[]> load(LocalDate from, LocalDate to, boolean flag) throws SQLException;
    }

    private interface Summary {
        String text(List<Object[]> rows);
    }

    private interface RowColor {
        Color of(Object[] row);
    }

    // ------------------------------------------------------------ one report

    private class ReportView extends JPanel {

        private final boolean useDates;
        private final Loader loader;
        private final Summary summary;
        private final RowColor colorizer;

        private final DefaultTableModel model;
        private final JTable table;
        private final JTextField fromField = new JTextField(9);
        private final JTextField toField = new JTextField(9);
        private final JCheckBox flagBox;
        private final JLabel summaryLabel = new JLabel(" ");
        private List<Object[]> rows = new ArrayList<>();

        ReportView(String[] columns, boolean useDates, String checkText,
                   Loader loader, Summary summary, RowColor colorizer) {
            this.useDates = useDates;
            this.loader = loader;
            this.summary = summary;
            this.colorizer = colorizer;
            this.flagBox = checkText == null ? null : new JCheckBox(checkText);

            model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);

            setLayout(new BorderLayout(8, 8));
            setBorder(new EmptyBorder(8, 8, 8, 8));
            add(buildFilterBar(), BorderLayout.NORTH);
            add(buildTable(), BorderLayout.CENTER);

            summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            summaryLabel.setBorder(new EmptyBorder(2, 4, 2, 4));
            add(summaryLabel, BorderLayout.SOUTH);

            generate();
        }

        private JPanel buildFilterBar() {
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

            if (useDates) {
                bar.add(new JLabel("From (yyyy-MM-dd):"));
                bar.add(fromField);
                bar.add(new JLabel("To:"));
                bar.add(toField);
                fromField.addActionListener(e -> generate());
                toField.addActionListener(e -> generate());
            }
            if (flagBox != null) {
                bar.add(flagBox);
                flagBox.addActionListener(e -> generate());
            }

            JButton generateBtn = styledButton(useDates ? "Generate" : "Refresh");
            generateBtn.addActionListener(e -> generate());
            bar.add(generateBtn);

            if (useDates) {
                JButton allBtn = styledButton("All Time");
                allBtn.addActionListener(e -> {
                    fromField.setText("");
                    toField.setText("");
                    generate();
                });
                bar.add(allBtn);
                JLabel hint = new JLabel("(leave blank for no limit)");
                hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
                bar.add(hint);
            }
            return bar;
        }

        private JScrollPane buildTable() {
            table.setRowHeight(24);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setGridColor(new Color(220, 220, 220));
            if ("ID".equals(model.getColumnName(0))) {
                table.getColumnModel().getColumn(0).setMaxWidth(50);
            }

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
                        if (colorizer != null && row < rows.size()) {
                            Color color = colorizer.of(rows.get(row));
                            if (color != null) {
                                c.setBackground(color);
                            }
                        }
                    }
                    return c;
                }
            });
            return new JScrollPane(table);
        }

        /** Runs the report query and fills the table and the summary line. */
        void generate() {
            LocalDate from = null;
            LocalDate to = null;
            if (useDates) {
                try {
                    from = parseDate(fromField.getText());
                    to = parseDate(toField.getText());
                } catch (DateTimeParseException ex) {
                    warn("Dates must be in the format yyyy-MM-dd, e.g. 2026-10-31");
                    return;
                }
                if (from != null && to != null && to.isBefore(from)) {
                    warn("The 'To' date cannot be before the 'From' date.");
                    return;
                }
            }
            try {
                rows = loader.load(from, to, flagBox != null && flagBox.isSelected());
                model.setRowCount(0);
                for (Object[] row : rows) {
                    model.addRow(row);
                }
                summaryLabel.setText(rows.isEmpty() ? "No records found." : summary.text(rows));
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Could not generate the report:\n" + e.getMessage(),
                        "Database error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private LocalDate parseDate(String text) {
            String t = text.trim();
            return t.isEmpty() ? null : LocalDate.parse(t);
        }

        private void warn(String message) {
            JOptionPane.showMessageDialog(this, message, "Check your input", JOptionPane.WARNING_MESSAGE);
        }

        private JButton styledButton(String text) {
            JButton b = new JButton(text);
            b.setBackground(PRIMARY);
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            return b;
        }
    }
}

package com.healthfirst.pims.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Queries behind the four admin reports. Every method returns plain table rows
 * (Object[]) in the same column order as the report screen shows them.
 */
public class ReportDAO {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Sales report: one row per sale.
     * Columns: Sale ID, Date, Cashier, Items Sold, Total.
     * from / to are optional (null = no limit) and both dates are inclusive.
     */
    public List<Object[]> salesReport(LocalDate from, LocalDate to) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT s.sale_id, s.sale_date, "
                + "COALESCE(u.full_name, '(deleted user)') AS cashier, "
                + "COALESCE((SELECT SUM(si.quantity_sold) FROM sale_items si "
                + "           WHERE si.sale_id = s.sale_id), 0) AS items_sold, "
                + "s.total_amount "
                + "FROM sales s LEFT JOIN users u ON s.user_id = u.user_id "
                + "WHERE 1 = 1" + dateConditions("s.sale_date", from, to, params)
                + " ORDER BY s.sale_date DESC, s.sale_id DESC";

        List<Object[]> rows = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("sale_date");
                    rows.add(new Object[]{
                        rs.getInt("sale_id"),
                        ts == null ? "" : ts.toLocalDateTime().format(DATE_TIME),
                        rs.getString("cashier"),
                        rs.getInt("items_sold"),
                        money(rs.getBigDecimal("total_amount"))
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Item-wise report: units sold and revenue per medicine (medicines with no sales show 0).
     * Columns: ID, Medicine, Type, Units Sold, Revenue. Best sellers first.
     */
    public List<Object[]> itemWiseReport(LocalDate from, LocalDate to) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT m.medicine_id, m.name, m.medicine_type, "
                + "COALESCE(SUM(x.quantity_sold), 0) AS units_sold, "
                + "COALESCE(SUM(x.quantity_sold * x.price_at_sale), 0) AS revenue "
                + "FROM medicines m "
                + "LEFT JOIN (SELECT si.medicine_id, si.quantity_sold, si.price_at_sale "
                + "           FROM sale_items si JOIN sales s ON s.sale_id = si.sale_id "
                + "           WHERE 1 = 1" + dateConditions("s.sale_date", from, to, params) + ") x "
                + "       ON x.medicine_id = m.medicine_id "
                + "GROUP BY m.medicine_id, m.name, m.medicine_type "
                + "ORDER BY units_sold DESC, m.name";

        List<Object[]> rows = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        nullToEmpty(rs.getString("medicine_type")),
                        rs.getInt("units_sold"),
                        money(rs.getBigDecimal("revenue"))
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Low stock report: medicines at or below their reorder level, lowest stock first.
     * Columns: ID, Medicine, Type, In Stock, Reorder Level, Shortfall, Supplier.
     */
    public List<Object[]> lowStockReport() throws SQLException {
        String sql = "SELECT m.medicine_id, m.name, m.medicine_type, m.quantity_in_stock, "
                + "m.reorder_level, (m.reorder_level - m.quantity_in_stock) AS shortfall, "
                + "COALESCE(sup.name, '') AS supplier "
                + "FROM medicines m LEFT JOIN suppliers sup ON m.supplier_id = sup.supplier_id "
                + "WHERE m.quantity_in_stock <= m.reorder_level "
                + "ORDER BY m.quantity_in_stock, m.name";

        List<Object[]> rows = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("medicine_id"),
                    rs.getString("name"),
                    nullToEmpty(rs.getString("medicine_type")),
                    rs.getInt("quantity_in_stock"),
                    rs.getInt("reorder_level"),
                    rs.getInt("shortfall"),
                    rs.getString("supplier")
                });
            }
        }
        return rows;
    }

    /**
     * Expiry report: medicines expiring within the next month (soonest first).
     * With includeExpired = true, medicines that have already expired are listed too.
     * Columns: ID, Medicine, Type, In Stock, Expiry Date, Days Left, Status, Supplier.
     */
    public List<Object[]> expiryReport(boolean includeExpired) throws SQLException {
        String sql = "SELECT m.medicine_id, m.name, m.medicine_type, m.quantity_in_stock, "
                + "m.expiry_date, DATEDIFF(m.expiry_date, CURDATE()) AS days_left, "
                + "COALESCE(sup.name, '') AS supplier "
                + "FROM medicines m LEFT JOIN suppliers sup ON m.supplier_id = sup.supplier_id "
                + "WHERE m.expiry_date IS NOT NULL "
                + "AND m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 1 MONTH) "
                + (includeExpired ? "" : "AND m.expiry_date >= CURDATE() ")
                + "ORDER BY m.expiry_date, m.name";

        List<Object[]> rows = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date expiry = rs.getDate("expiry_date");
                int daysLeft = rs.getInt("days_left");
                String status;
                if (daysLeft < 0) {
                    status = "Expired";
                } else if (daysLeft == 0) {
                    status = "Expires today";
                } else if (daysLeft == 1) {
                    status = "Expires tomorrow";
                } else {
                    status = "Expires in " + daysLeft + " days";
                }
                rows.add(new Object[]{
                    rs.getInt("medicine_id"),
                    rs.getString("name"),
                    nullToEmpty(rs.getString("medicine_type")),
                    rs.getInt("quantity_in_stock"),
                    expiry.toLocalDate().toString(),
                    daysLeft,
                    status,
                    rs.getString("supplier")
                });
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------- helpers

    /** Builds " AND col >= ? AND col < ?" for an inclusive from/to date range. */
    private String dateConditions(String column, LocalDate from, LocalDate to, List<Object> params) {
        StringBuilder sb = new StringBuilder();
        if (from != null) {
            sb.append(" AND ").append(column).append(" >= ?");
            params.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sb.append(" AND ").append(column).append(" < ?");
            params.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        return sb.toString();
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

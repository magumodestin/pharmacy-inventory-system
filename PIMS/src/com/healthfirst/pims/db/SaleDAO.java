package com.healthfirst.pims.db;

import com.healthfirst.pims.model.Sale;
import com.healthfirst.pims.model.SaleItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * Data-access class for sales. saveSale() runs as ONE database transaction:
 * either the sale header, every line item and every stock reduction are saved,
 * or nothing is (so stock and sales can never get out of step).
 */
public class SaleDAO {

    /**
     * Saves a sale for the given cashier and reduces stock.
     *
     * @return the saved Sale (with its generated sale_id, date and total)
     * @throws SQLException if a medicine has expired, has too little stock,
     *                      or a database error occurs. The message is safe to show to the user.
     */
    public Sale saveSale(int userId, List<SaleItem> items) throws SQLException {
        Connection conn = DBConnection.getConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1. Lock and validate every medicine in the cart
            BigDecimal total = BigDecimal.ZERO;
            String checkSql = "SELECT name, quantity_in_stock, expiry_date "
                    + "FROM medicines WHERE medicine_id = ? FOR UPDATE";
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                for (SaleItem item : items) {
                    check.setInt(1, item.getMedicineId());
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(item.getMedicineName() + " no longer exists in the system.");
                        }
                        String name = rs.getString("name");
                        int stock = rs.getInt("quantity_in_stock");
                        Date expiry = rs.getDate("expiry_date");

                        if (expiry != null && expiry.toLocalDate().isBefore(LocalDate.now())) {
                            throw new SQLException(name + " has expired and cannot be sold.");
                        }
                        if (item.getQuantitySold() > stock) {
                            throw new SQLException("Not enough stock for " + name
                                    + " (only " + stock + " left).");
                        }
                    }
                    total = total.add(item.getLineTotal());
                }
            }

            // 2. Insert the sale header
            int saleId;
            String headerSql = "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(headerSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setBigDecimal(1, total);
                ps.setInt(2, userId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Could not create the sale record.");
                    }
                    saleId = keys.getInt(1);
                }
            }

            // 3. Insert the line items and reduce stock
            String itemSql = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) "
                    + "VALUES (?, ?, ?, ?)";
            String stockSql = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? "
                    + "WHERE medicine_id = ?";
            try (PreparedStatement insertItem = conn.prepareStatement(itemSql);
                 PreparedStatement reduceStock = conn.prepareStatement(stockSql)) {
                for (SaleItem item : items) {
                    insertItem.setInt(1, saleId);
                    insertItem.setInt(2, item.getMedicineId());
                    insertItem.setInt(3, item.getQuantitySold());
                    insertItem.setBigDecimal(4, item.getPriceAtSale());
                    insertItem.executeUpdate();

                    reduceStock.setInt(1, item.getQuantitySold());
                    reduceStock.setInt(2, item.getMedicineId());
                    reduceStock.executeUpdate();
                }
            }

            // 4. Read back the timestamp the database assigned
            Sale sale = new Sale();
            sale.setSaleId(saleId);
            sale.setTotalAmount(total);
            sale.setUserId(userId);
            try (PreparedStatement ps = conn.prepareStatement("SELECT sale_date FROM sales WHERE sale_id = ?")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp("sale_date");
                        if (ts != null) {
                            sale.setSaleDate(ts.toLocalDateTime());
                        }
                    }
                }
            }

            conn.commit();
            return sale;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // nothing more we can do
            }
        }
    }
}

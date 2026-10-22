package com.healthfirst.pims.db;

import com.healthfirst.pims.model.Medicine;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access class for the medicines table (full CRUD + search).
 * Methods throw SQLException so the UI can show a meaningful message.
 */
public class MedicineDAO {

    private static final String BASE_SELECT =
            "SELECT m.*, s.name AS supplier_name FROM medicines m "
          + "LEFT JOIN suppliers s ON m.supplier_id = s.supplier_id ";

    public List<Medicine> getAll() throws SQLException {
        return fetch(BASE_SELECT + "ORDER BY m.name");
    }

    /** Matches the keyword against name, company and medicine type. */
    public List<Medicine> search(String keyword) throws SQLException {
        String sql = BASE_SELECT
                + "WHERE m.name LIKE ? OR m.company LIKE ? OR m.medicine_type LIKE ? "
                + "ORDER BY m.name";
        String like = "%" + keyword + "%";
        return fetch(sql, like, like, like);
    }

    public void add(Medicine m) throws SQLException {
        String sql = "INSERT INTO medicines (name, company, medicine_type, price, "
                + "quantity_in_stock, reorder_level, expiry_date, supplier_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, m);
            ps.executeUpdate();
        }
    }

    public void update(Medicine m) throws SQLException {
        String sql = "UPDATE medicines SET name = ?, company = ?, medicine_type = ?, price = ?, "
                + "quantity_in_stock = ?, reorder_level = ?, expiry_date = ?, supplier_id = ? "
                + "WHERE medicine_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, m);
            ps.setInt(9, m.getMedicineId());
            ps.executeUpdate();
        }
    }

    public void delete(int medicineId) throws SQLException {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- helpers

    private List<Medicine> fetch(String sql, String... params) throws SQLException {
        List<Medicine> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Binds parameters 1-8 shared by INSERT and UPDATE. */
    private void bind(PreparedStatement ps, Medicine m) throws SQLException {
        ps.setString(1, m.getName());
        ps.setString(2, m.getCompany());
        ps.setString(3, m.getMedicineType());
        ps.setBigDecimal(4, m.getPrice());
        ps.setInt(5, m.getQuantityInStock());
        ps.setInt(6, m.getReorderLevel());
        ps.setDate(7, Date.valueOf(m.getExpiryDate()));
        if (m.getSupplierId() > 0) {
            ps.setInt(8, m.getSupplierId());
        } else {
            ps.setNull(8, Types.INTEGER);
        }
    }

    private Medicine mapRow(ResultSet rs) throws SQLException {
        Date expiry = rs.getDate("expiry_date");
        Medicine m = new Medicine(
                rs.getInt("medicine_id"),
                rs.getString("name"),
                rs.getString("company"),
                rs.getString("medicine_type"),
                rs.getBigDecimal("price"),
                rs.getInt("quantity_in_stock"),
                rs.getInt("reorder_level"),
                expiry == null ? null : expiry.toLocalDate(),
                rs.getInt("supplier_id")
        );
        m.setSupplierName(rs.getString("supplier_name"));
        return m;
    }
}

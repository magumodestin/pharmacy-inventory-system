package com.healthfirst.pims.db;

import com.healthfirst.pims.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access class for the users table: login check + admin management of cashier accounts.
 * Management methods throw SQLException so the UI can show a meaningful message
 * (e.g. error code 1062 = duplicate username).
 */
public class UserDAO {

    /** Returns the matching User if credentials are correct, otherwise null. */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try {
            Connection conn = DBConnection.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** All accounts with the Cashier role. */
    public List<User> getCashiers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'Cashier' ORDER BY full_name";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void addUser(User u) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.executeUpdate();
        }
    }

    /** Updates name and username; the password is only changed when updatePassword is true. */
    public void updateUser(User u, boolean updatePassword) throws SQLException {
        Connection conn = DBConnection.getConnection();
        if (updatePassword) {
            String sql = "UPDATE users SET username = ?, full_name = ?, password = ? WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getFullName());
                ps.setString(3, u.getPassword());
                ps.setInt(4, u.getUserId());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE users SET username = ?, full_name = ? WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getFullName());
                ps.setInt(3, u.getUserId());
                ps.executeUpdate();
            }
        }
    }

    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ? AND role = 'Cashier'";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("full_name")
        );
    }
}

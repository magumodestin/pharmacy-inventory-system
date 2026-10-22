package com.healthfirst.pims.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place for getting a JDBC connection to the pims_db database.
 * Edit DB_USER / DB_PASSWORD (and the host/port in the URLs) to match your local MySQL setup.
 * XAMPP's default is user "root" with an empty password.
 */
public class DBConnection {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/pims_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    /** Same server, but not tied to a database - used to create pims_db the first time. */
    private static final String SERVER_URL =
            "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // set your MySQL root password here

    private static Connection connection;

    private DBConnection() {
        // utility class - no instances
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = open(DB_URL);
        }
        return connection;
    }

    /** A connection to the MySQL server without selecting a database. The caller must close it. */
    public static Connection getServerConnection() throws SQLException {
        return open(SERVER_URL);
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection open(String url) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. "
                    + "Make sure mysql-connector-j is added to the project libraries.", e);
        }
    }
}

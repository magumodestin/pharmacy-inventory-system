package com.healthfirst.pims.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the application easy to test: on start-up it checks that MySQL is running and,
 * if the pims_db database does not exist yet, creates it (tables + sample data) from the
 * database.sql script bundled inside the application.
 */
public class DatabaseSetup {

    /** database.sql sits next to this class (same package folder) so it is packed into the .jar. */
    private static final String SCRIPT_RESOURCE = "database.sql";

    private DatabaseSetup() {
        // utility class - no instances
    }

    /**
     * Ensures MySQL is reachable and pims_db exists.
     *
     * @throws SQLException with a message that is safe to show to the user
     */
    public static void ensureReady() throws SQLException {
        try {
            DBConnection.getConnection(); // works when the server is up and pims_db exists
            return;
        } catch (SQLException e) {
            if (!isUnknownDatabase(e)) {
                throw new SQLException("Could not connect to the MySQL server on localhost:3306.\n"
                        + "Please start MySQL (for example in the XAMPP Control Panel) and try again.\n\n"
                        + "Details: " + e.getMessage(), e);
            }
        }

        // The server is running but pims_db is missing: create it from the bundled script.
        runScript(readScript());
        DBConnection.getConnection(); // confirm the new database can be opened
    }

    // ---------------------------------------------------------------- helpers

    /** MySQL error 1049 = "Unknown database". */
    private static boolean isUnknownDatabase(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException && ((SQLException) t).getErrorCode() == 1049) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && message.toLowerCase().contains("unknown database")) {
                return true;
            }
        }
        return false;
    }

    private static String readScript() throws SQLException {
        try (InputStream in = DatabaseSetup.class.getResourceAsStream(SCRIPT_RESOURCE)) {
            if (in == null) {
                throw new SQLException("The database 'pims_db' does not exist yet and the bundled "
                        + "database.sql script was not found.\nPlease import database.sql manually "
                        + "(for example with phpMyAdmin) and start the application again.");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new SQLException("Could not read the bundled database.sql script: " + e.getMessage(), e);
        }
    }

    private static void runScript(String script) throws SQLException {
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            for (String sql : splitStatements(script)) {
                st.execute(sql);
            }
        }
    }

    /** Splits the script into statements: '--' comment lines are skipped, a line ending in ';' ends one. */
    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1));
                current.setLength(0);
            }
        }
        return statements;
    }
}

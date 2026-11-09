package com.carrental.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates all required tables (per the approved ERD) before the CLI starts. */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String users = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    name          TEXT NOT NULL,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role          TEXT NOT NULL CHECK (role IN ('ADMIN','STAFF','CUSTOMER')),
                    status        TEXT NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','APPROVED','INACTIVE'))
                )
                """;

        String cars = """
                CREATE TABLE IF NOT EXISTS cars (
                    car_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    brand         TEXT NOT NULL,
                    model         TEXT NOT NULL,
                    plate_number  TEXT NOT NULL UNIQUE,
                    price_per_day REAL NOT NULL,
                    status        TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
                """;

        String rentals = """
                CREATE TABLE IF NOT EXISTS rentals (
                    rental_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id            INTEGER NOT NULL,
                    car_id             INTEGER NOT NULL,
                    rental_date        TIMESTAMP,
                    return_date        TIMESTAMP,
                    actual_return_date TIMESTAMP,
                    total_amount       REAL,
                    late_fee           REAL DEFAULT 0,
                    status             TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(user_id),
                    FOREIGN KEY (car_id)  REFERENCES cars(car_id)
                )
                """;

        // Audit trail (Deliverable 8). Deleting a user also deletes that user's log entries (ON DELETE CASCADE).
        String activityLogs = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp   TIMESTAMP NOT NULL DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """;

        // Parent tables first so the foreign keys can be created.
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(users);
            st.execute(cars);
            st.execute(rentals);
            upgradeLegacyActivityLogs(conn);
            st.execute(activityLogs);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }

        // Default admin so someone can approve PENDING accounts.
        UserDao dao = new UserDao();
        if (!dao.usernameExists("admin")) {
            dao.register("System Admin", "admin", "admin123", "ADMIN", "APPROVED");
        }
    }

    /**
     * Older databases were created with an earlier activity_logs layout (columns "action" / "log_timestamp").
     * Nothing wrote to that table before Deliverable 8, so it is dropped and re-created with the new layout.
     */
    private static void upgradeLegacyActivityLogs(Connection conn) throws SQLException {
        boolean exists = false;
        boolean hasActionType = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(activity_logs)")) {
            while (rs.next()) {
                exists = true;
                if ("action_type".equalsIgnoreCase(rs.getString("name"))) {
                    hasActionType = true;
                }
            }
        }
        if (exists && !hasActionType) {
            try (Statement st = conn.createStatement()) {
                st.execute("DROP TABLE activity_logs");
            }
        }
    }
}

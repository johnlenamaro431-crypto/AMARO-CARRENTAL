package com.carrental.dao;

import java.sql.Connection;
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

        String activityLogs = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id       INTEGER NOT NULL,
                    action        TEXT,
                    description   TEXT,
                    log_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
                """;

        // Parent tables first so the foreign keys can be created.
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(users);
            st.execute(cars);
            st.execute(rentals);
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
}

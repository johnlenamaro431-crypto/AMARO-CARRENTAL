package com.carrental.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite connection and creates the schema on first run.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:carrental.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }


    public static void initialize() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL CHECK (roles IN ('ADMIN','STAFF','CUSTOMER')),
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','ACTIVE','INACTIVE'))
                )
                """;

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(createUsers);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }


        UserDao dao = new UserDao();
        if (!dao.usernameExists("admin")) {
            dao.register("admin", "admin123", "ADMIN", "ACTIVE");
        }
    }
}

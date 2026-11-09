package com.carrental.dao;

import com.carrental.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the activity_logs (audit trail) table lives here. Every query is parameterized. */
public class ActivityLogDao {

    private static final String SELECT_LOGS =
            "SELECT log_id, user_id, action_type, description, timestamp FROM activity_logs ";
    private static final String NEWEST_FIRST = "ORDER BY timestamp DESC, log_id DESC";

    /** Records one audit entry on its own connection. Returns true if the row was inserted. */
    public boolean logAction(int userId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return logAction(conn, userId, actionType, description);
        } catch (SQLException e) {
            System.out.println("Audit log error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Records one audit entry on an existing connection, so a DAO can save its business change and the
     * matching audit entry in the SAME transaction (both are saved, or neither is).
     */
    public boolean logAction(Connection conn, int userId, String actionType, String description)
            throws SQLException {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            return ps.executeUpdate() == 1;
        }
    }

    /** Every recorded action, newest first. */
    public List<ActivityLog> getAllLogs() {
        return query(SELECT_LOGS + NEWEST_FIRST, null);
    }

    /** Every action performed by one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT_LOGS + "WHERE user_id = ? " + NEWEST_FIRST, userId);
    }

    /**
     * Deletes all historical log entries and, in the same transaction, records a LOGS_CLEARED entry so the
     * clearing itself is never lost. Returns how many entries were deleted, or -1 on failure.
     */
    public int clearAllLogs(int adminId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM activity_logs")) {
                int deleted = ps.executeUpdate();
                logAction(conn, adminId, "LOGS_CLEARED",
                        "Admin " + adminId + " cleared " + deleted + " audit log entries");
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return -1;
        }
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("log_id"),
                            rs.getInt("user_id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }
}

package com.carrental.dao;

import com.carrental.model.User;
import com.carrental.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL for the users table lives here. */
public class UserDao {

    private final ActivityLogDao activityLogDao;

    public UserDao() {
        this(new ActivityLogDao());
    }

    public UserDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** Registers a new account with PENDING status. */
    public boolean register(String name, String username, String password, String role) {
        return register(name, username, password, role, "PENDING");
    }

    public boolean register(String name, String username, String password, String role, String status) {
        String sql = "INSERT INTO users (name, username, password_hash, role, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.setString(5, status);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Returns the user if the username/password match (any status; the UI decides what to do). */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT user_id, name, username, password_hash, role, status FROM users "
                + "WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Returns every registered account, regardless of role or status. */
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, name, username, password_hash, role, status FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return users;
    }

    /** Returns all accounts (any role) still waiting for approval. */
    public List<User> getPendingUsers() {
        String sql = "SELECT user_id, name, username, password_hash, role, status FROM users "
                + "WHERE status = ? ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return users;
    }

    /** Finds one account by its ID (used to validate IDs typed in menus). */
    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, name, username, password_hash, role, status FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Changes a PENDING account to APPROVED and logs USER_APPROVED (in the same transaction).
     * Returns true if a row was updated.
     */
    public boolean approveUser(int adminId, int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE user_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
                activityLogDao.logAction(conn, adminId, "USER_APPROVED",
                        "Admin " + adminId + " approved account for User ID " + userId);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the role and status of one account and logs USER_UPDATED (in the same transaction).
     * Inputs are upper-cased before they reach the database. Returns true if exactly one row was updated.
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        String role = newRole.toUpperCase();
        String status = newStatus.toUpperCase();
        String sql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, role);
                ps.setString(2, status);
                ps.setInt(3, userId);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
                activityLogDao.logAction(conn, adminId, "USER_UPDATED",
                        "Admin " + adminId + " changed role of User " + userId + " to " + role
                                + ", status to " + status);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("status"));
    }
}

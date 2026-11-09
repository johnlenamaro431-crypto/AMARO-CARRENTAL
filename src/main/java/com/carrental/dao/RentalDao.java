package com.carrental.dao;

import com.carrental.model.Rental;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * All SQL for the rentals table lives here. Each business change (rental + car status) is saved together with
 * its audit entry in ONE transaction.
 */
public class RentalDao {

    private static final String SELECT_RENTAL =
            "SELECT rental_id, user_id, car_id, rental_date, return_date, total_amount, status FROM rentals ";

    private final ActivityLogDao activityLogDao;

    public RentalDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /**
     * CORE BUSINESS PROCESS: rents an AVAILABLE car to a customer for a number of days.
     * Creates the ACTIVE rental, marks the car RENTED and logs RENTAL_CREATED.
     * Returns the new rental, or empty if the car is no longer available.
     */
    public Optional<Rental> processRental(int actorId, int customerId, int carId, int days) {
        String findCar = "SELECT brand, model, price_per_day FROM cars WHERE car_id = ? AND status = 'AVAILABLE'";
        String markRented = "UPDATE cars SET status = 'RENTED' WHERE car_id = ? AND status = 'AVAILABLE'";
        String insert = "INSERT INTO rentals (user_id, car_id, rental_date, return_date, total_amount, status) "
                + "VALUES (?, ?, datetime('now','localtime'), datetime('now','localtime', ?), ?, 'ACTIVE')";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String carName;
                double total;
                try (PreparedStatement ps = conn.prepareStatement(findCar)) {
                    ps.setInt(1, carId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Optional.empty();
                        }
                        carName = rs.getString("brand") + " " + rs.getString("model");
                        total = rs.getDouble("price_per_day") * days;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(markRented)) {
                    ps.setInt(1, carId);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return Optional.empty();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, carId);
                    ps.setString(3, "+" + days + " days");
                    ps.setDouble(4, total);
                    ps.executeUpdate();
                }

                int rentalId;
                try (PreparedStatement ps = conn.prepareStatement("SELECT last_insert_rowid()");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    rentalId = rs.getInt(1);
                }

                activityLogDao.logAction(conn, actorId, "RENTAL_CREATED",
                        String.format(Locale.US, "Processed rental of %s for User %d - Total: P%.2f",
                                carName, customerId, total));

                Rental created = findById(conn, rentalId);
                conn.commit();
                return Optional.ofNullable(created);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * LIFECYCLE / STATE CHANGE: moves an ACTIVE rental to RETURNED or CANCELLED, frees the car again and logs
     * RENTAL_UPDATED. Returns false if the rental is not ACTIVE or the new status is not allowed.
     */
    public boolean updateRentalStatus(int actorId, int rentalId, String newStatus) {
        if (!"RETURNED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
            return false;
        }
        String findRental = "SELECT car_id, status FROM rentals WHERE rental_id = ?";
        String updateRental = "UPDATE rentals SET status = ?, "
                + "actual_return_date = CASE WHEN ? = 'RETURNED' THEN datetime('now','localtime') ELSE NULL END "
                + "WHERE rental_id = ? AND status = 'ACTIVE'";
        String freeCar = "UPDATE cars SET status = 'AVAILABLE' WHERE car_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int carId;
                String oldStatus;
                try (PreparedStatement ps = conn.prepareStatement(findRental)) {
                    ps.setInt(1, rentalId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        carId = rs.getInt("car_id");
                        oldStatus = rs.getString("status");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateRental)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, newStatus);
                    ps.setInt(3, rentalId);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(freeCar)) {
                    ps.setInt(1, carId);
                    ps.executeUpdate();
                }

                activityLogDao.logAction(conn, actorId, "RENTAL_UPDATED",
                        "Updated Rental #" + rentalId + " status from " + oldStatus + " to " + newStatus);
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

    /** Rentals that are still out (not yet returned or cancelled). */
    public List<Rental> getActiveRentals() {
        List<Rental> rentals = new ArrayList<>();
        String sql = SELECT_RENTAL + "WHERE status = ? ORDER BY rental_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapRental(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return rentals;
    }

    private Rental findById(Connection conn, int rentalId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_RENTAL + "WHERE rental_id = ?")) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRental(rs) : null;
            }
        }
    }

    private Rental mapRental(ResultSet rs) throws SQLException {
        return new Rental(
                rs.getInt("rental_id"),
                rs.getInt("user_id"),
                rs.getInt("car_id"),
                rs.getString("rental_date"),
                rs.getString("return_date"),
                rs.getDouble("total_amount"),
                rs.getString("status"));
    }
}

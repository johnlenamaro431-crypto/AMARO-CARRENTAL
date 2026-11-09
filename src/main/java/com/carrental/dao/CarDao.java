package com.carrental.dao;

import com.carrental.model.Car;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the cars table lives here. */
public class CarDao {

    private final ActivityLogDao activityLogDao;

    public CarDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** Adds a car (status defaults to AVAILABLE) and logs CAR_ADDED. Returns false if the plate already exists. */
    public boolean addCar(int actorId, String brand, String model, String plateNumber, double pricePerDay) {
        String sql = "INSERT INTO cars (brand, model, plate_number, price_per_day) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, brand);
                ps.setString(2, model);
                ps.setString(3, plateNumber);
                ps.setDouble(4, pricePerDay);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
                activityLogDao.logAction(conn, actorId, "CAR_ADDED",
                        "User " + actorId + " added car " + brand + " " + model + " (" + plateNumber + ")");
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

    public List<Car> getAllCars() {
        return query("SELECT car_id, brand, model, plate_number, price_per_day, status FROM cars ORDER BY car_id",
                null);
    }

    public List<Car> getAvailableCars() {
        return query("SELECT car_id, brand, model, plate_number, price_per_day, status FROM cars "
                + "WHERE status = ? ORDER BY car_id", "AVAILABLE");
    }

    private List<Car> query(String sql, String status) {
        List<Car> cars = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cars.add(new Car(
                            rs.getInt("car_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("plate_number"),
                            rs.getDouble("price_per_day"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return cars;
    }
}

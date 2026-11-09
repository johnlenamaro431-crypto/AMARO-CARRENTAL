package com.carrental.model;

/** Maps to the rentals table in the ERD (userId is the customer who rented the car). */
public record Rental(int rentalId, int userId, int carId, String rentalDate, String returnDate,
                     double totalAmount, String status) {
}

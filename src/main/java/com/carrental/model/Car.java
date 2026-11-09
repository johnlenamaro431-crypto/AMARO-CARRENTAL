package com.carrental.model;

/** Maps to the cars table in the ERD. */
public record Car(int carId, String brand, String model, String plateNumber, double pricePerDay, String status) {
}

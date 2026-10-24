package com.carrental.model;

/** Maps to the users table in the ERD. */
public record User(int userId, String name, String username, String passwordHash, String role, String status) {
}

package com.carrental.ui;

import com.carrental.dao.UserDao;
import com.carrental.model.User;

import java.util.Optional;
import java.util.Scanner;

/** Handles the login / registration screens and routes users to their role menu. No SQL here. */
public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n==============================");
            System.out.println("      CAR RENTAL SYSTEM");
            System.out.println("==============================");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Thank you for using the Car Rental System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1-3.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- REGISTER ---");

        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.print("Password (min 6 characters): ");
        String password = scanner.nextLine();
        if (password.length() < 6) {
            System.out.println("Password is too short.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. CUSTOMER");
        System.out.println("2. STAFF");
        System.out.println("3. ADMIN");
        System.out.print("Choose: ");
        String role;
        switch (scanner.nextLine().trim()) {
            case "1" -> role = "CUSTOMER";
            case "2" -> role = "STAFF";
            case "3" -> role = "ADMIN";
            default -> {
                System.out.println("Invalid role selection.");
                return;
            }
        }

        if (userDao.register(name, username, password, role)) {
            System.out.println("Registration successful! Role: " + role + " | Status: PENDING");
        } else {
            System.out.println("Registration failed.");
        }
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.login(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        if (!"APPROVED".equals(user.status())) {
            System.out.println("Your account is " + user.status() + ". Please wait for admin approval.");
            return;
        }

        System.out.println("\nLogin successful! Welcome, " + user.name()
                + " [" + user.role() + " | " + user.status() + "]");

        switch (user.role()) {
            case "ADMIN" -> new AdminMenu(scanner, userDao, user).show();
            case "STAFF" -> new StaffMenu(scanner, userDao, user).show();
            case "CUSTOMER" -> new CustomerMenu(scanner, userDao, user).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }
}

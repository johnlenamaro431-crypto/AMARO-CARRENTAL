package com.carrental.ui;

import com.carrental.dao.UserDao;

import java.util.Scanner;


public class RegisterScreen {

    private final Scanner scanner;
    private final UserDao userDao = new UserDao();

    public RegisterScreen(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("\n--- REGISTER ---");

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
        System.out.print("Choose: ");
        String roleChoice = scanner.nextLine().trim();
        String role;
        switch (roleChoice) {
            case "1" -> role = "CUSTOMER";
            case "2" -> role = "STAFF";
            default -> {
                System.out.println("Invalid role selection.");
                return;
            }
        }

        if (userDao.register(username, password, role)) {
            System.out.println("Registration successful! Role: " + role + " | Status: PENDING");
        } else {
            System.out.println("Registration failed.");
        }
    }
}

package com.carrental.ui;

import com.carrental.model.User;
import java.util.Scanner;

public class DashboardMenu {

    private final Scanner scanner;
    private final User user;

    public DashboardMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username()
                + " [" + user.roles() + " | " + user.status() + "]");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- " + user.roles() + " DASHBOARD ---");
            switch (user.roles()) {
                case "ADMIN" -> System.out.println("(coming soon) Manage cars, prices and accounts");
                case "STAFF" -> System.out.println("(coming soon) Confirm rentals, hand over cars, record returns");
                default -> System.out.println("(coming soon) Browse cars, rent a car, view/cancel bookings");
            }
            System.out.println("1. Logout");
            System.out.print("Choose: ");

            if (scanner.nextLine().trim().equals("1")) {
                System.out.println("Logged out. Goodbye, " + user.username() + "!");
                loggedIn = false;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}
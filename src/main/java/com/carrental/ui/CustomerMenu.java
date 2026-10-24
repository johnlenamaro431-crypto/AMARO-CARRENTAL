package com.carrental.ui;

import com.carrental.dao.UserDao;
import com.carrental.model.User;

import java.util.Scanner;

/** CUSTOMER menu (placeholder until the CUSTOMER features are built). */
public class CustomerMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public CustomerMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- CUSTOMER MENU ---");
            System.out.println("(coming soon) Browse cars, rent a car, view/cancel bookings");
            System.out.println("1. Logout");
            System.out.print("Choose: ");

            if (scanner.nextLine().trim().equals("1")) {
                System.out.println("Logged out. Goodbye, " + user.name() + "!");
                loggedIn = false;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}

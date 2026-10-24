package com.carrental.ui;

import com.carrental.dao.UserDao;
import com.carrental.model.User;

import java.util.Scanner;

/** STAFF menu (placeholder until the STAFF features are built). */
public class StaffMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public StaffMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- STAFF MENU ---");
            System.out.println("(coming soon) Confirm rentals, hand over cars, record returns");
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

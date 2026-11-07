package com.carrental.ui;

import com.carrental.dao.UserDao;
import com.carrental.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Admin menu: view users, approve pending accounts and update a user's role/status.
 * Includes input validation wrappers. No SQL here.
 */
public class AdminMenu {

    private static final List<String> VALID_ROLES = List.of("CUSTOMER", "STAFF", "ADMIN");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public AdminMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User Role & Status");
            System.out.println("5. Logout");

            switch (readIntInput("Choose: ")) {
                case 1 -> {
                    System.out.println("\n=== ALL SYSTEM USERS ===");
                    printUserTable(userDao.getAllUsers());
                }
                case 2 -> {
                    System.out.println("\n=== PENDING REGISTRATIONS ===");
                    printUserTable(userDao.getPendingUsers());
                }
                case 3 -> approveUser();
                case 4 -> updateUser();
                case 5 -> {
                    System.out.println("Logged out. Goodbye, " + user.name() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1-5.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Update role & status
    // ------------------------------------------------------------------

    private void updateUser() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n=== UPDATE USER ROLE & STATUS ===");
        printUserTable(users);

        User target;
        while (true) {
            int targetId = readIntInput("Enter Target User ID to update (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (targetId == user.userId()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            target = findById(users, targetId);
            if (target == null) {
                System.out.println("User ID " + targetId + " does not exist. Try again.");
                continue;
            }
            break;
        }

        System.out.println("Updating " + target.username() + " (current: " + target.role()
                + " | " + target.status() + ")");

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(target.userId(), newRole, newStatus)) {
            System.out.println("Success! " + target.username() + " is now " + newRole + " | " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // Input validation wrappers
    // ------------------------------------------------------------------

    /** Keeps asking until the user types a whole number. Letters/symbols never crash the program. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Returns a valid role (upper-case), or null if the admin enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + VALID_ROLES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Choose one of: " + String.join(", ", VALID_ROLES));
        }
    }

    /** Returns a valid status (upper-case), or null if the admin enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + VALID_STATUSES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Choose one of: " + String.join(", ", VALID_STATUSES));
        }
    }

    // ------------------------------------------------------------------
    // Approve (existing feature)
    // ------------------------------------------------------------------

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n=== APPROVE USER ACCOUNT ===");
        printUserTable(pending);
        if (pending.isEmpty()) {
            return;
        }

        while (true) {
            int targetId = readIntInput("Enter Target User ID to approve (0 to cancel): ");
            if (targetId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = findById(pending, targetId);
            if (target == null) {
                System.out.println("User ID " + targetId + " is not in the pending list. Try again.");
                continue;
            }

            if (userDao.approveUser(targetId)) {
                System.out.println("Success! " + target.username() + " (" + target.role()
                        + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    private User findById(List<User> users, int id) {
        for (User u : users) {
            if (u.userId() == id) {
                return u;
            }
        }
        return null;
    }

    /** Prints any list of users as an aligned table. */
    private void printUserTable(List<User> users) {
        String border = "+-------+-----------------+--------------+------------+";
        String row = "| %-5s | %-15s | %-12s | %-10s |%n";

        System.out.println(border);
        System.out.printf(row, "ID", "Username", "Role", "Status");
        System.out.println(border);
        if (users.isEmpty()) {
            System.out.printf("| %-51s |%n", "No users found.");
        } else {
            for (User u : users) {
                System.out.printf(row, u.userId(), u.username(), u.role(), u.status());
            }
        }
        System.out.println(border);
    }
}

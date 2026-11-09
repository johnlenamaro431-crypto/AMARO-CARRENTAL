package com.carrental.ui;

import com.carrental.dao.CarDao;
import com.carrental.dao.RentalDao;
import com.carrental.dao.UserDao;
import com.carrental.model.Car;
import com.carrental.model.Rental;
import com.carrental.model.User;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * STAFF menu: manage the car fleet and process rentals. Every action here is written to the audit log by the
 * DAO layer. Includes input validation wrappers. No SQL here.
 */
public class StaffMenu {

    private static final int MAX_RENTAL_DAYS = 365;

    private final Scanner scanner;
    private final UserDao userDao;
    private final CarDao carDao;
    private final RentalDao rentalDao;
    private final User user;

    public StaffMenu(Scanner scanner, UserDao userDao, CarDao carDao, RentalDao rentalDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.carDao = carDao;
        this.rentalDao = rentalDao;
        this.user = user;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- STAFF MENU ---");
            System.out.println("1. Add Car");
            System.out.println("2. View All Cars");
            System.out.println("3. Process Rental");
            System.out.println("4. View Active Rentals");
            System.out.println("5. Update Rental Status (Return / Cancel)");
            System.out.println("6. Logout");

            switch (readIntInput("Choose: ")) {
                case 1 -> addCar();
                case 2 -> {
                    System.out.println("\n=== ALL CARS ===");
                    printCarTable(carDao.getAllCars());
                }
                case 3 -> processRental();
                case 4 -> {
                    System.out.println("\n=== ACTIVE RENTALS ===");
                    printRentalTable(rentalDao.getActiveRentals());
                }
                case 5 -> updateRentalStatus();
                case 6 -> {
                    System.out.println("Logged out. Goodbye, " + user.name() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1-6.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Add car
    // ------------------------------------------------------------------

    private void addCar() {
        System.out.println("\n=== ADD CAR (enter 0 at any prompt to cancel) ===");
        String brand = readText("Brand: ");
        if (brand == null) {
            System.out.println("Add car cancelled.");
            return;
        }
        String model = readText("Model: ");
        if (model == null) {
            System.out.println("Add car cancelled.");
            return;
        }
        String plate = readText("Plate number: ");
        if (plate == null) {
            System.out.println("Add car cancelled.");
            return;
        }
        Double price = readPositiveDouble("Price per day: ");
        if (price == null) {
            System.out.println("Add car cancelled.");
            return;
        }

        if (carDao.addCar(user.userId(), brand, model, plate.toUpperCase(), price)) {
            System.out.println("Success! " + brand + " " + model + " (" + plate.toUpperCase() + ") was added.");
        } else {
            System.out.println("Could not add the car. The plate number may already exist.");
        }
    }

    // ------------------------------------------------------------------
    // Process rental (core business process)
    // ------------------------------------------------------------------

    private void processRental() {
        List<Car> available = carDao.getAvailableCars();
        System.out.println("\n=== PROCESS RENTAL ===");
        printCarTable(available);
        if (available.isEmpty()) {
            return;
        }

        Car car;
        while (true) {
            int carId = readIntInput("Enter Car ID to rent (0 to cancel): ");
            if (carId == 0) {
                System.out.println("Rental cancelled.");
                return;
            }
            car = findCar(available, carId);
            if (car != null) {
                break;
            }
            System.out.println("Car ID " + carId + " is not in the available list. Try again.");
        }

        User customer;
        while (true) {
            int customerId = readIntInput("Enter Customer User ID (0 to cancel): ");
            if (customerId == 0) {
                System.out.println("Rental cancelled.");
                return;
            }
            Optional<User> found = userDao.findById(customerId);
            if (found.isEmpty()) {
                System.out.println("User ID " + customerId + " does not exist. Try again.");
            } else if (!"CUSTOMER".equals(found.get().role())) {
                System.out.println("User ID " + customerId + " is not a CUSTOMER account. Try again.");
            } else if (!"APPROVED".equals(found.get().status())) {
                System.out.println("User ID " + customerId + " is not APPROVED yet. Try again.");
            } else {
                customer = found.get();
                break;
            }
        }

        int days;
        while (true) {
            days = readIntInput("Number of rental days (1-" + MAX_RENTAL_DAYS + ", 0 to cancel): ");
            if (days == 0) {
                System.out.println("Rental cancelled.");
                return;
            }
            if (days >= 1 && days <= MAX_RENTAL_DAYS) {
                break;
            }
            System.out.println("Days must be between 1 and " + MAX_RENTAL_DAYS + ".");
        }

        Optional<Rental> rental = rentalDao.processRental(user.userId(), customer.userId(), car.carId(), days);
        if (rental.isPresent()) {
            Rental r = rental.get();
            System.out.println("Success! Rental #" + r.rentalId() + " created for " + customer.username()
                    + " | Total: P" + money(r.totalAmount()) + " | Due: " + r.returnDate());
        } else {
            System.out.println("Rental could not be processed. The car may no longer be available.");
        }
    }

    // ------------------------------------------------------------------
    // Update rental status (lifecycle / state change)
    // ------------------------------------------------------------------

    private void updateRentalStatus() {
        List<Rental> active = rentalDao.getActiveRentals();
        System.out.println("\n=== UPDATE RENTAL STATUS ===");
        printRentalTable(active);
        if (active.isEmpty()) {
            return;
        }

        Rental target;
        while (true) {
            int rentalId = readIntInput("Enter Rental ID to update (0 to cancel): ");
            if (rentalId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            target = findRental(active, rentalId);
            if (target != null) {
                break;
            }
            System.out.println("Rental ID " + rentalId + " is not in the active list. Try again.");
        }

        String newStatus;
        while (true) {
            System.out.println("1. RETURNED");
            System.out.println("2. CANCELLED");
            int choice = readIntInput("Choose new status (0 to cancel): ");
            if (choice == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (choice == 1) {
                newStatus = "RETURNED";
                break;
            }
            if (choice == 2) {
                newStatus = "CANCELLED";
                break;
            }
            System.out.println("Invalid choice. Please enter 1 or 2.");
        }

        if (rentalDao.updateRentalStatus(user.userId(), target.rentalId(), newStatus)) {
            System.out.println("Success! Rental #" + target.rentalId() + " is now " + newStatus + ".");
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

    /** Returns non-empty text, or null if the user enters 0 to cancel. */
    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** Returns a number greater than zero, or null if the user enters 0 to cancel. */
    private Double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value == 0) {
                    return null;
                }
                if (value > 0 && !Double.isInfinite(value)) {
                    return value;
                }
                System.out.println("Please enter an amount greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers & tables
    // ------------------------------------------------------------------

    private Car findCar(List<Car> cars, int id) {
        for (Car c : cars) {
            if (c.carId() == id) {
                return c;
            }
        }
        return null;
    }

    private Rental findRental(List<Rental> rentals, int id) {
        for (Rental r : rentals) {
            if (r.rentalId() == id) {
                return r;
            }
        }
        return null;
    }

    private String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private void printCarTable(List<Car> cars) {
        String border = "+-------+--------------+--------------+------------+------------+------------+";
        String row = "| %-5s | %-12s | %-12s | %-10s | %-10s | %-10s |%n";

        System.out.println(border);
        System.out.printf(row, "ID", "Brand", "Model", "Plate", "Rate/Day", "Status");
        System.out.println(border);
        if (cars.isEmpty()) {
            System.out.printf("| %-" + (border.length() - 4) + "s |%n", "No cars found.");
        } else {
            for (Car c : cars) {
                System.out.printf(row, c.carId(), c.brand(), c.model(), c.plateNumber(),
                        money(c.pricePerDay()), c.status());
            }
        }
        System.out.println(border);
    }

    private void printRentalTable(List<Rental> rentals) {
        String border = "+-------+----------+-------+---------------------+---------------------+------------+";
        String row = "| %-5s | %-8s | %-5s | %-19s | %-19s | %-10s |%n";

        System.out.println(border);
        System.out.printf(row, "ID", "Customer", "Car", "Rented", "Due", "Total");
        System.out.println(border);
        if (rentals.isEmpty()) {
            System.out.printf("| %-" + (border.length() - 4) + "s |%n", "No active rentals.");
        } else {
            for (Rental r : rentals) {
                System.out.printf(row, r.rentalId(), r.userId(), r.carId(), r.rentalDate(), r.returnDate(),
                        money(r.totalAmount()));
            }
        }
        System.out.println(border);
    }
}

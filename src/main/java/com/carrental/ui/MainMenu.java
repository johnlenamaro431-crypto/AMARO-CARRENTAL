package com.carrental.ui;

import java.util.Scanner;


public class MainMenu {

    private final Scanner scanner;
    private final RegisterScreen registerScreen;
    private final LoginScreen loginScreen;

    public MainMenu(Scanner scanner) {
        this.scanner = scanner;
        this.registerScreen = new RegisterScreen(scanner);
        this.loginScreen = new LoginScreen(scanner);
    }

    public void show() {
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
                case "1" -> registerScreen.show();
                case "2" -> loginScreen.show();
                case "3" -> {
                    System.out.println("Thank you for using the Car Rental System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1-3.");
            }
        }
    }
}

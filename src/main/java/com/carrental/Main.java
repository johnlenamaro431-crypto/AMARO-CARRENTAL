package com.carrental;

import com.carrental.dao.DatabaseConnection;
import com.carrental.ui.MainMenu;

import java.util.Scanner;

/** Application entry point. */
public class    Main {
    public static void main(String[] args) {
        DatabaseConnection.initialize();
        try (Scanner scanner = new Scanner(System.in)) {
            new MainMenu(scanner).show();
        }
    }
}

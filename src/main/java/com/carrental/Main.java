package com.carrental;

import com.carrental.dao.DatabaseInitializer;
import com.carrental.dao.UserDao;
import com.carrental.ui.Authentication;

import java.util.Scanner;

/** Application entry point: initialize DB -> create shared DAOs/utilities -> hand over to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserDao userDao = new UserDao();
        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(userDao, scanner).start();
        }
    }
}

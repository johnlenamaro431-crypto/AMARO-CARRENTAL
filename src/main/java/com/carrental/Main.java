package com.carrental;

import com.carrental.dao.ActivityLogDao;
import com.carrental.dao.CarDao;
import com.carrental.dao.DatabaseInitializer;
import com.carrental.dao.RentalDao;
import com.carrental.dao.UserDao;
import com.carrental.ui.Authentication;

import java.util.Scanner;

/** Application entry point: initialize DB -> create shared DAOs/utilities -> hand over to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        ActivityLogDao activityLogDao = new ActivityLogDao();
        UserDao userDao = new UserDao(activityLogDao);
        CarDao carDao = new CarDao(activityLogDao);
        RentalDao rentalDao = new RentalDao(activityLogDao);
        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(userDao, carDao, rentalDao, activityLogDao, scanner).start();
        }
    }
}

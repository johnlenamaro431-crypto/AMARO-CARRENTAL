# Car Rental System

Layered Java console app with SQLite (Maven).

## Structure
```
src/main/java/com/carrental
├── Main.java                 (entry point only)
├── dao    DatabaseConnection, DatabaseInitializer, UserDao
├── model  User (record)
├── util   PasswordUtil (SHA-256)
└── ui     Authentication, AdminMenu, StaffMenu, CustomerMenu
```

## Run
```
mvn compile exec:java
```
Default admin: `admin` / `admin123` (status ACTIVE).
New registrations (CUSTOMER, STAFF or ADMIN) are saved as PENDING and cannot log in until approved.

## Database
`carrental.db` is created automatically on first run (tables: users, cars, rentals, activity_logs).
SQLite foreign keys are enabled on every connection (`PRAGMA foreign_keys = ON`).
Passwords are stored as SHA-256 hashes in `users.password_hash`.

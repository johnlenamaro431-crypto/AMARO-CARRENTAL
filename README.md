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
Default admin: `admin` / `admin123` (status APPROVED).
New registrations (CUSTOMER, STAFF or ADMIN) are saved as PENDING and cannot log in until an admin approves them (status APPROVED).

## Database
`carrental.db` is created automatically on first run (tables: users, cars, rentals, activity_logs).
SQLite foreign keys are enabled on every connection (`PRAGMA foreign_keys = ON`).
Passwords are stored as SHA-256 hashes in `users.password_hash`.

## Admin menu
View all users, view pending registrations, and approve a pending account by User ID (enter 0 to cancel).

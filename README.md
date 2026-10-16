# Car Rental System

Layered Java console app with SQLite (Maven).

## Structure
```
src/main/java/com/carrental
├── Main.java
├── dao    DatabaseConnection.java, UserDao.java
├── model  User.java (record)
└── ui     MainMenu, RegisterScreen, LoginScreen, DashboardMenu
```

## Run
```
mvn compile exec:java
```
Default admin: `admin` / `admin123` (status ACTIVE).
New registrations (CUSTOMER or STAFF) are saved as PENDING.

## Database
`carrental.db` is created automatically on first run.
`users(id, username, password, roles, status)`; passwords stored as SHA-256 hashes.

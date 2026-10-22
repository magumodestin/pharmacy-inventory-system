# Pharmacy Inventory Management System (PIMS)

A desktop application for **HealthFirst Pharmacy** that replaces manual record-keeping. It manages medicine stock, processes sales with printable bills, and gives the owner reports on sales, low stock and expiring medicines. Built with **Java Swing**, **JDBC** and **MySQL**.

## Features

**Login and roles**
- Secure login screen with role-based redirection (Admin or Cashier)
- Clear error messages for failed logins

**Administrator**
- Medicines: add, view, update, delete and search
- Suppliers: full CRUD
- Users: create, delete and manage cashier accounts
- Reports: Sales, Item-Wise, Low Stock and Expiry (medicines expiring within the next month)

**Cashier**
- Point of Sale: search medicines, build a cart, remove items, clear the cart, checkout
- Bill window for each sale (print or save)
- Stock Check: look up price and availability without making a sale (read-only)
- Expired and out-of-stock medicines cannot be sold

## Tech stack

| Part | Technology |
|------|------------|
| Language | Java |
| GUI | Swing / AWT |
| Database access | JDBC (MySQL Connector/J) |
| Database | MySQL |
| IDE | Apache NetBeans |

## Database

Database name: `pims_db`, with five tables: `users`, `suppliers`, `medicines`, `sales`, `sale_items`. The script `database.sql` creates everything and adds sample data. The app also runs it automatically the first time it starts.

## Getting started

**Requirements:** Java 11 or newer, and MySQL running on `localhost:3306` (for example through XAMPP).

1. Start MySQL.
2. Clone or download this repository.
3. Run it either way:
   - Open the project in NetBeans and press Run, or
   - Build the project, then run `java -jar dist/PIMS.jar` (keep the `lib` folder next to the jar).
4. If your MySQL root password is not empty, change `DB_PASSWORD` in `src/com/healthfirst/pims/db/DBConnection.java`.

## Default logins

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Cashier | `cashier` | `cash123` |

## Project structure

```
src/com/healthfirst/pims/
  ui/      Swing screens (login, dashboards, panels, bill window)
  db/      database access (DAO classes, connection, setup script)
  model/   data classes (User, Medicine, Supplier, Sale, SaleItem)
screenshots/   screenshots of the application
```

## Screenshots

| Login | Admin dashboard | Cashier |
|-------|-----------------|---------|
| ![Login](screenshots/login.jpg) | ![Admin](screenshots/admin.jpg) | ![Cashier](screenshots/cashier_dashbord.jpg) |

## About

Created as a university assignment (Programming 632) at Richfield Graduate Institute of Technology.

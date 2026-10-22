=====================================================
 HealthFirst Pharmacy - Pharmacy Inventory Management System (PIMS)
 Programming 632 Assignment
=====================================================

DEFAULT LOGIN CREDENTIALS
-------------------------
Administrator :  admin    / admin123
Cashier       :  cashier  / cash123


REQUIREMENTS
------------
- Java 11 or newer installed
- MySQL Server running on localhost:3306
  (for example XAMPP: start "MySQL" in the XAMPP Control Panel)
- MySQL user "root" with an empty password (XAMPP default).
  If your root password is different, change DB_PASSWORD in
  src/com/healthfirst/pims/db/DBConnection.java and rebuild.


HOW TO RUN
----------
1. Start MySQL (XAMPP Control Panel > MySQL > Start).
2. Double-click  Destine_pims.exe
   (keep the "lib" folder and PIMS.jar in the same folder as the .exe)
3. The first time it starts, the program creates the database "pims_db"
   with all tables and sample data automatically.
   You can also create it manually by running database.sql in
   phpMyAdmin or MySQL Workbench.
4. Log in with one of the accounts above.

Alternative: open a command prompt in this folder and run
   java -jar PIMS.jar


HOW TO USE
----------
LOGIN
  Enter your username and password. Administrators go to the Admin
  Dashboard and cashiers go to the Cashier Dashboard. A wrong username or
  password shows an error message.

ADMIN DASHBOARD (tabs)
  Manage Medicines : add, update, delete and search medicines.
  Manage Suppliers : add, update and delete suppliers.
  Manage Users     : create, delete and manage cashier accounts.
  Reports          : Sales Report, Item-Wise Report, Low Stock Report and
                     Expiry Report (medicines expiring in the next month).

CASHIER DASHBOARD (tabs)
  Point of Sale : search for a medicine, choose a quantity and click
                  "Add to Cart" (or double-click a row). Use "Remove Selected"
                  or "Clear Cart" to edit the cart. "Checkout" saves the sale,
                  reduces the stock and shows the bill window.
                  Expired and out-of-stock medicines cannot be sold.
  Stock Check   : look up the price and availability of a medicine without
                  making a sale. Cashiers cannot add or edit medicines.


DATABASE
--------
Database name : pims_db
Tables        : users, suppliers, medicines, sales, sale_items
Script        : database.sql (creates the database, tables and sample data)


FOLDER CONTENTS
---------------
Destine_pims.exe   the program (needs PIMS.jar and lib/ beside it)
PIMS.jar, lib/     program and MySQL driver
src/               all .java source files
database.sql       SQL script (create database, tables, sample data)
screenshots/       screenshots of the application
README.txt         this file
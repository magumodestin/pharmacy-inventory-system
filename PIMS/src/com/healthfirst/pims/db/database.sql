-- ============================================================
-- HealthFirst Pharmacy Inventory Management System (PIMS)
-- Database Script: schema + sample data
-- ============================================================

DROP DATABASE IF EXISTS pims_db;
CREATE DATABASE pims_db;
USE pims_db;

-- ------------------------------------------------------------
-- Table: users
-- ------------------------------------------------------------
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('Admin', 'Cashier') NOT NULL,
    full_name   VARCHAR(100) NOT NULL
);

-- ------------------------------------------------------------
-- Table: suppliers
-- ------------------------------------------------------------
CREATE TABLE suppliers (
    supplier_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         TEXT
);

-- ------------------------------------------------------------
-- Table: medicines
-- ------------------------------------------------------------
CREATE TABLE medicines (
    medicine_id         INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    company             VARCHAR(100),
    medicine_type       VARCHAR(50),
    price               DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity_in_stock   INT NOT NULL DEFAULT 0,
    reorder_level       INT NOT NULL DEFAULT 10,
    expiry_date         DATE,
    supplier_id         INT,
    CONSTRAINT fk_medicine_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(supplier_id)
        ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Table: sales
-- ------------------------------------------------------------
CREATE TABLE sales (
    sale_id       INT AUTO_INCREMENT PRIMARY KEY,
    sale_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    user_id       INT,
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Table: sale_items
-- ------------------------------------------------------------
CREATE TABLE sale_items (
    sale_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    sale_id         INT NOT NULL,
    medicine_id     INT NOT NULL,
    quantity_sold   INT NOT NULL,
    price_at_sale   DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_saleitem_sale FOREIGN KEY (sale_id)
        REFERENCES sales(sale_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_saleitem_medicine FOREIGN KEY (medicine_id)
        REFERENCES medicines(medicine_id)
);

-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Default users (plain text for demo purposes; hash in production)
INSERT INTO users (username, password, role, full_name) VALUES
('admin',   'admin123', 'Admin',   'System Administrator'),
('cashier', 'cash123',  'Cashier', 'Front Counter Cashier');

-- Suppliers
INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
('MedSupply Co.',      'John Naidoo',   '011-555-0101', 'john@medsupply.co.za',   '12 Main Rd, Johannesburg'),
('PharmaDirect',       'Sarah Khumalo', '021-555-0202', 'sarah@pharmadirect.co.za','45 Long St, Cape Town'),
('HealthLine Distributors', 'Peter van Wyk', '031-555-0303', 'peter@healthline.co.za', '8 Umgeni Rd, Durban');

-- Medicines (mix of expiry dates: some soon, some far, for the expiry report)
INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
('Paracetamol 500mg',   'Adcock Ingram', 'Tablet',    25.50, 200, 30, '2027-06-30', 1),
('Amoxicillin 250mg',   'Aspen Pharma',  'Capsule',   45.00, 15,  20, DATE_ADD(CURDATE(), INTERVAL 20 DAY), 1),
('Cough Syrup 100ml',   'Cipla',         'Syrup',     60.00, 40,  15, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 2),
('Insulin Injection',   'Novo Nordisk',  'Injection', 210.00, 8,  10, '2026-12-01', 2),
('Hydrocortisone Cream','GSK',           'Cream',     35.00, 25,  10, '2028-01-15', 3),
('Ibuprofen 200mg',     'Adcock Ingram', 'Tablet',    30.00, 5,   20, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 1),
('Vitamin C 1000mg',    'Pharma Dynamics','Tablet',   50.00, 100, 25, '2027-03-20', 3);

-- Sample sale (so reports have something to show)
INSERT INTO sales (total_amount, user_id) VALUES (111.00, 2);
INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES
(1, 1, 2, 25.50),
(1, 3, 1, 60.00);

DROP DATABASE IF EXISTS azani_db;
CREATE DATABASE azani_db;
USE azani_db;

CREATE TABLE institution (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(20) NOT NULL,
    location        VARCHAR(100),
    contact_name    VARCHAR(100) NOT NULL,
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(100),
    reg_date        DATE NOT NULL,
    mbps            INT NOT NULL,
    upgraded        INT NOT NULL DEFAULT 0,
    num_users       INT NOT NULL DEFAULT 0,
    pcs             INT NOT NULL DEFAULT 0,
    nodes           INT NOT NULL DEFAULT 0,
    site_ready      INT NOT NULL DEFAULT 0,
    pc_cost         DECIMAL(10,2) NOT NULL,
    lan_cost        DECIMAL(10,2) NOT NULL,
    install_cost    DECIMAL(10,2) NOT NULL,
    monthly_charge  DECIMAL(10,2) NOT NULL
);

CREATE TABLE bill (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    institution_id   INT NOT NULL,
    bill_month       VARCHAR(7) NOT NULL,
    amount           DECIMAL(10,2) NOT NULL,
    due_date         DATE NOT NULL,
    paid             INT NOT NULL DEFAULT 0,
    paid_date        DATE,
    fine             DECIMAL(10,2) NOT NULL DEFAULT 0,
    reconnection_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    UNIQUE (institution_id, bill_month),
    FOREIGN KEY (institution_id) REFERENCES institution(id)
);

CREATE TABLE payment (
    payment_id     INT AUTO_INCREMENT PRIMARY KEY,
    institution_id INT NOT NULL,
    type           VARCHAR(20) NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    pay_date       DATE NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id)
);

INSERT INTO institution (name, category, location, contact_name, contact_phone, contact_email,
    reg_date, mbps, upgraded, num_users, pcs, nodes, site_ready, pc_cost, lan_cost, install_cost, monthly_charge)
VALUES
('Mwangaza Primary School', 'Primary', 'Kasarani', 'Jane Wanjiru', '0711000001', 'jane@mwangaza.ac.ke',
    '2026-06-02', 4, 0, 60, 10, 10, 0, 400000, 10000, 420000, 1200),
('Kijani Junior School', 'Junior', 'Thika', 'Peter Otieno', '0711000002', 'peter@kijani.ac.ke',
    '2026-06-10', 10, 0, 120, 0, 0, 1, 0, 0, 10000, 2000),
('St. Peters Senior School', 'Senior', 'Nakuru', 'Mary Achieng', '0711000003', 'mary@stpeters.ac.ke',
    '2026-06-15', 25, 1, 300, 5, 25, 0, 200000, 30000, 240000, 3600),
('Nairobi Tech College', 'College', 'Nairobi CBD', 'David Kamau', '0711000004', 'david@nairobitech.ac.ke',
    '2026-06-20', 50, 1, 800, 0, 60, 0, 0, 40000, 50000, 6300),
('Sunrise Primary School', 'Primary', 'Ruiru', 'Grace Mutua', '0711000005', 'grace@sunrise.ac.ke',
    '2026-07-01', 4, 0, 40, 3, 2, 0, 120000, 10000, 140000, 1200);

INSERT INTO bill (institution_id, bill_month, amount, due_date, paid, paid_date, fine, reconnection_fee) VALUES
(1, '2026-08', 1200, '2026-08-31', 1, '2026-08-28', 0, 0),
(2, '2026-08', 2000, '2026-08-31', 0, NULL, 0, 0),
(3, '2026-08', 3600, '2026-08-31', 1, '2026-09-05', 540, 0),
(4, '2026-08', 6300, '2026-08-31', 1, '2026-08-30', 0, 0),
(5, '2026-08', 1200, '2026-08-31', 1, '2026-09-15', 180, 1000),
(1, '2026-09', 1200, '2026-09-30', 0, NULL, 0, 0),
(2, '2026-09', 2000, '2026-09-30', 0, NULL, 0, 0),
(3, '2026-09', 3600, '2026-09-30', 1, '2026-09-29', 0, 0),
(4, '2026-09', 6300, '2026-09-30', 1, '2026-09-28', 0, 0),
(5, '2026-09', 1200, '2026-09-30', 0, NULL, 0, 0);

INSERT INTO payment (institution_id, type, amount, pay_date) VALUES
(1, 'REGISTRATION', 8500, '2026-06-02'), (2, 'REGISTRATION', 8500, '2026-06-10'),
(3, 'REGISTRATION', 8500, '2026-06-15'), (4, 'REGISTRATION', 8500, '2026-06-20'),
(5, 'REGISTRATION', 8500, '2026-07-01'),
(1, 'INSTALLATION', 420000, '2026-06-20'), (2, 'INSTALLATION', 10000, '2026-06-25'),
(3, 'INSTALLATION', 240000, '2026-07-01'), (4, 'INSTALLATION', 50000, '2026-07-05'),
(5, 'INSTALLATION', 140000, '2026-07-15'),
(1, 'MONTHLY', 1200, '2026-08-28'),
(3, 'MONTHLY', 3600, '2026-09-05'), (3, 'FINE', 540, '2026-09-05'),
(4, 'MONTHLY', 6300, '2026-08-30'),
(5, 'MONTHLY', 1200, '2026-09-15'), (5, 'FINE', 180, '2026-09-15'), (5, 'RECONNECTION', 1000, '2026-09-15'),
(3, 'MONTHLY', 3600, '2026-09-29'),
(4, 'MONTHLY', 6300, '2026-09-28');

CREATE DATABASE IF NOT EXISTS civiceye;
USE civiceye;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    category VARCHAR(40) NOT NULL,
    location VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    photo_path VARCHAR(500),
    status ENUM('PENDING', 'UNDER REVIEW', 'IN PROGRESS', 'RESOLVED')
        NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    resolution_note VARCHAR(1000),
    CONSTRAINT fk_complaint_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_location ON complaints(location);

-- Run this file in MySQL Workbench before starting the Java program.
-- Demo admin login is configured in Main.java:
-- username: admin
-- password: Admin@123

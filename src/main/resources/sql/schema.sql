-- ====================================================================
-- Online Personal Finance Management Platform - Database Schema
-- Compatible with MySQL 8.x / MariaDB
-- ====================================================================

CREATE DATABASE IF NOT EXISTS finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE finance_db;

-- 1. Users Table
-- Stores user accounts, credentials, role (ADMIN / USER), and status (ACTIVE / SUSPENDED)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Expenses Table
-- Stores user expenses categorized by amount, category, date, and description
CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    category VARCHAR(50) NOT NULL,
    expense_date VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    created_at VARCHAR(30) NOT NULL,
    CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Budgets Table
-- Stores category budget limits and durations (Monthly, Weekly, Yearly)
CREATE TABLE IF NOT EXISTS budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount_limit DOUBLE NOT NULL,
    duration VARCHAR(30) NOT NULL,
    created_at VARCHAR(30) NOT NULL,
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Financial Goals Table
-- Stores savings targets, current progress, deadlines, and achievement status
CREATE TABLE IF NOT EXISTS goals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    description VARCHAR(200) NOT NULL,
    target_amount DOUBLE NOT NULL,
    current_amount DOUBLE NOT NULL DEFAULT 0.0,
    deadline VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at VARCHAR(30) NOT NULL,
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Security Settings Table
-- Stores demo configuration for data encryption and access-control policies
CREATE TABLE IF NOT EXISTS security_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    encryption_standard VARCHAR(100) NOT NULL,
    tls_required INT NOT NULL DEFAULT 1,
    session_timeout_minutes INT NOT NULL DEFAULT 30,
    mfa_enforcement VARCHAR(30) NOT NULL DEFAULT 'OPTIONAL',
    lockout_threshold INT NOT NULL DEFAULT 5,
    last_updated_by VARCHAR(100) NOT NULL,
    updated_at VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. System Settings Table
-- Stores global platform parameters, default currency, and alert thresholds
CREATE TABLE IF NOT EXISTS system_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    platform_name VARCHAR(150) NOT NULL,
    default_currency VARCHAR(30) NOT NULL DEFAULT 'USD ($)',
    maintenance_mode INT NOT NULL DEFAULT 0,
    budget_alert_threshold INT NOT NULL DEFAULT 80,
    last_updated_by VARCHAR(100) NOT NULL,
    updated_at VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Audit & Activity Logs Table
-- Records administrative changes and security policy events
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    log_type VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    details VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50),
    created_at VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

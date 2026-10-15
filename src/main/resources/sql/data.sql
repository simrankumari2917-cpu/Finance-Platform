-- ====================================================================
-- Online Personal Finance Management Platform - Sample Seed Data
-- ====================================================================

USE finance_db;

-- Passwords hashed using SHA-256 with static salt 'CollegeFinanceProjectSalt_2026!'
-- Admin@123  -> 684e20980ca51c056d61dcdecfda9c450a808ecbb56a6ec15b1fb68093db5fcf
-- User@123   -> cdfd44bb0cbca08e235a90947ba9fe1d960f2d43e527d4fc487e500aa9cb31fd

-- 1. Initial Accounts (Admin & Regular Student User)
INSERT INTO users (id, full_name, email, password_hash, role, status, created_at)
VALUES
(1, 'Administrator', 'admin@finance.edu', '684e20980ca51c056d61dcdecfda9c450a808ecbb56a6ec15b1fb68093db5fcf', 'ADMIN', 'ACTIVE', '2026-10-01 09:00:00'),
(2, 'Alex Student', 'student@college.edu', 'cdfd44bb0cbca08e235a90947ba9fe1d960f2d43e527d4fc487e500aa9cb31fd', 'USER', 'ACTIVE', '2026-10-01 09:30:00')
ON DUPLICATE KEY UPDATE id=id;

-- 2. Sample Expenses for User 2
INSERT INTO expenses (id, user_id, amount, category, expense_date, description, created_at)
VALUES
(1, 2, 120.50, 'Education', '2026-10-04', 'CS Textbooks & Reference Materials', '2026-10-04 10:15:00'),
(2, 2, 45.00, 'Food & Dining', '2026-10-06', 'Weekly Grocery Shopping', '2026-10-06 14:20:00'),
(3, 2, 18.75, 'Food & Dining', '2026-10-07', 'Campus Cafeteria Lunch', '2026-10-07 12:45:00'),
(4, 2, 50.00, 'Transportation', '2026-10-02', 'Monthly Public Bus Pass', '2026-10-02 08:30:00'),
(5, 2, 29.99, 'Entertainment', '2026-10-08', 'Music & Video Streaming Subscriptions', '2026-10-08 19:10:00'),
(6, 2, 35.00, 'Utilities', '2026-09-28', 'High-speed Internet Bill share', '2026-09-28 11:00:00')
ON DUPLICATE KEY UPDATE id=id;

-- 3. Sample Budgets for User 2
INSERT INTO budgets (id, user_id, category, amount_limit, duration, created_at)
VALUES
(1, 2, 'Food & Dining', 200.00, 'Monthly', '2026-10-01 10:00:00'),
(2, 2, 'Transportation', 80.00, 'Monthly', '2026-10-01 10:00:00'),
(3, 2, 'Education', 250.00, 'Monthly', '2026-10-01 10:00:00'),
(4, 2, 'Entertainment', 60.00, 'Monthly', '2026-10-01 10:00:00')
ON DUPLICATE KEY UPDATE id=id;

-- 4. Sample Goals for User 2
INSERT INTO goals (id, user_id, description, target_amount, current_amount, deadline, status, created_at)
VALUES
(1, 2, 'Emergency Savings Fund', 1000.00, 450.00, '2027-04-30', 'IN_PROGRESS', '2026-10-01 10:05:00'),
(2, 2, 'New Study Laptop', 750.00, 750.00, '2026-11-15', 'COMPLETED', '2026-10-01 10:05:00'),
(3, 2, 'Summer Internship Travel Fund', 500.00, 150.00, '2027-02-28', 'IN_PROGRESS', '2026-10-01 10:05:00')
ON DUPLICATE KEY UPDATE id=id;

-- 5. Baseline Security Settings
INSERT INTO security_settings (id, encryption_standard, tls_required, session_timeout_minutes, mfa_enforcement, lockout_threshold, last_updated_by, updated_at)
VALUES
(1, 'AES-256 (Simulated Academic Mode)', 1, 30, 'OPTIONAL', 5, 'admin@finance.edu', '2026-10-01 09:00:00')
ON DUPLICATE KEY UPDATE id=id;

-- 6. Baseline System Settings
INSERT INTO system_settings (id, platform_name, default_currency, maintenance_mode, budget_alert_threshold, last_updated_by, updated_at)
VALUES
(1, 'CampusFin Personal Finance Platform', 'USD ($)', 0, 80, 'admin@finance.edu', '2026-10-01 09:00:00')
ON DUPLICATE KEY UPDATE id=id;

-- 7. Audit & Event Activity Logs
INSERT INTO audit_logs (id, log_type, action, performed_by, details, ip_address, created_at)
VALUES
(1, 'SYSTEM', 'SYSTEM_INIT', 'SYSTEM', 'College Finance Platform schema and seed data initialized', '127.0.0.1', '2026-10-01 09:00:00'),
(2, 'SECURITY', 'SECURITY_BASELINE', 'admin@finance.edu', 'Baseline security policy established (AES-256 demo, TLS active, 30m timeout)', '127.0.0.1', '2026-10-01 09:05:00')
ON DUPLICATE KEY UPDATE id=id;

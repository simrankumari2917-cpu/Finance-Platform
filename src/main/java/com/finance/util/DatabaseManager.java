package com.finance.util;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Manages database connectivity, schema creation, and initial sample data seeding.
 * Supports MySQL as primary relational database with automatic fallback to embedded SQLite.
 */
public class DatabaseManager {

    private static String dbType = "mysql";
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbDriver;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Could not read db.properties, using defaults: " + e.getMessage());
        }

        dbType = props.getProperty("db.type", "mysql").trim().toLowerCase();

        if ("mysql".equals(dbType)) {
            dbUrl = props.getProperty("db.mysql.url", "jdbc:mysql://localhost:3306/finance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dbUser = props.getProperty("db.mysql.username", "root");
            dbPassword = props.getProperty("db.mysql.password", "root");
            dbDriver = props.getProperty("db.mysql.driver", "com.mysql.cj.jdbc.Driver");
        } else {
            dbUrl = props.getProperty("db.sqlite.url", "jdbc:sqlite:finance.db");
            dbUser = "";
            dbPassword = "";
            dbDriver = props.getProperty("db.sqlite.driver", "org.sqlite.JDBC");
        }
    }

    /**
     * Obtains a connection to the configured database.
     * If MySQL fails to connect, gracefully switches to embedded SQLite to keep the platform operational.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(dbDriver);
            if ("mysql".equalsIgnoreCase(dbType)) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } else {
                return DriverManager.getConnection(dbUrl);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseManager] Driver class not found: " + dbDriver + ". Attempting fallback to SQLite...");
            return switchToSqliteFallback();
        } catch (SQLException e) {
            if ("mysql".equalsIgnoreCase(dbType)) {
                System.err.println("[DatabaseManager] MySQL connection failed (" + e.getMessage() + ").");
                System.err.println("[DatabaseManager] Seamlessly switching to embedded SQLite (finance.db) for uninterrupted operation.");
                return switchToSqliteFallback();
            }
            throw e;
        }
    }

    private static synchronized Connection switchToSqliteFallback() throws SQLException {
        try {
            dbType = "sqlite";
            dbDriver = "org.sqlite.JDBC";
            dbUrl = "jdbc:sqlite:finance.db";
            dbUser = "";
            dbPassword = "";
            Class.forName(dbDriver);
            return DriverManager.getConnection(dbUrl);
        } catch (Exception ex) {
            throw new SQLException("Failed to establish fallback SQLite connection: " + ex.getMessage(), ex);
        }
    }

    public static String getActiveDatabaseType() {
        return dbType.toUpperCase();
    }

    /**
     * Initializes database tables and default sample data if not already present.
     */
    public static void initializeDatabase() {
        System.out.println("[DatabaseManager] Testing database connection and creating tables if needed...");
        try (Connection conn = getConnection()) {
            System.out.println("[DatabaseManager] Connected successfully to " + getActiveDatabaseType() + " database.");
            createTables(conn);
            seedInitialData(conn);
            System.out.println("[DatabaseManager] Database initialized and verified.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        boolean isMySQL = "mysql".equalsIgnoreCase(dbType);
        String autoInc = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String primaryKeyDef = isMySQL ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";

        try (Statement stmt = conn.createStatement()) {
            // 1. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id " + primaryKeyDef + ", " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL DEFAULT 'USER', " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at VARCHAR(30) NOT NULL" +
                    ")");

            // 2. Expenses Table
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id " + primaryKeyDef + ", " +
                    "user_id INT NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "expense_date VARCHAR(20) NOT NULL, " +
                    "description VARCHAR(255), " +
                    "created_at VARCHAR(30) NOT NULL" +
                    ")");

            // 3. Budgets Table
            stmt.execute("CREATE TABLE IF NOT EXISTS budgets (" +
                    "id " + primaryKeyDef + ", " +
                    "user_id INT NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "amount_limit DOUBLE NOT NULL, " +
                    "duration VARCHAR(30) NOT NULL, " +
                    "created_at VARCHAR(30) NOT NULL" +
                    ")");

            // 4. Financial Goals Table
            stmt.execute("CREATE TABLE IF NOT EXISTS goals (" +
                    "id " + primaryKeyDef + ", " +
                    "user_id INT NOT NULL, " +
                    "description VARCHAR(200) NOT NULL, " +
                    "target_amount DOUBLE NOT NULL, " +
                    "current_amount DOUBLE NOT NULL DEFAULT 0.0, " +
                    "deadline VARCHAR(20) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', " +
                    "created_at VARCHAR(30) NOT NULL" +
                    ")");

            // 5. Security Settings Table
            stmt.execute("CREATE TABLE IF NOT EXISTS security_settings (" +
                    "id " + primaryKeyDef + ", " +
                    "encryption_standard VARCHAR(100) NOT NULL, " +
                    "tls_required INT NOT NULL DEFAULT 1, " +
                    "session_timeout_minutes INT NOT NULL DEFAULT 30, " +
                    "mfa_enforcement VARCHAR(30) NOT NULL DEFAULT 'OPTIONAL', " +
                    "lockout_threshold INT NOT NULL DEFAULT 5, " +
                    "last_updated_by VARCHAR(100) NOT NULL, " +
                    "updated_at VARCHAR(30) NOT NULL" +
                    ")");

            // 6. System Settings Table
            stmt.execute("CREATE TABLE IF NOT EXISTS system_settings (" +
                    "id " + primaryKeyDef + ", " +
                    "platform_name VARCHAR(150) NOT NULL, " +
                    "default_currency VARCHAR(30) NOT NULL DEFAULT 'USD ($)', " +
                    "maintenance_mode INT NOT NULL DEFAULT 0, " +
                    "budget_alert_threshold INT NOT NULL DEFAULT 80, " +
                    "last_updated_by VARCHAR(100) NOT NULL, " +
                    "updated_at VARCHAR(30) NOT NULL" +
                    ")");

            // 7. Audit & Activity Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id " + primaryKeyDef + ", " +
                    "log_type VARCHAR(20) NOT NULL, " +
                    "action VARCHAR(100) NOT NULL, " +
                    "performed_by VARCHAR(100) NOT NULL, " +
                    "details VARCHAR(255) NOT NULL, " +
                    "ip_address VARCHAR(50), " +
                    "created_at VARCHAR(30) NOT NULL" +
                    ")");
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Check if users exist
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        System.out.println("[DatabaseManager] Seeding initial sample data (Admin & User accounts)...");
        String now = LocalDateTime.now().toString().replace("T", " ");

        // 1. Initial Users
        String adminHash = PasswordUtil.hashPassword("Admin@123");
        String userHash = PasswordUtil.hashPassword("User@123");

        String insertUserSql = "INSERT INTO users (full_name, email, password_hash, role, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql)) {
            // Admin
            pstmt.setString(1, "Administrator");
            pstmt.setString(2, "admin@finance.edu");
            pstmt.setString(3, adminHash);
            pstmt.setString(4, "ADMIN");
            pstmt.setString(5, "ACTIVE");
            pstmt.setString(6, now);
            pstmt.executeUpdate();

            // Demo Student User
            pstmt.setString(1, "Alex Student");
            pstmt.setString(2, "student@college.edu");
            pstmt.setString(3, userHash);
            pstmt.setString(4, "USER");
            pstmt.setString(5, "ACTIVE");
            pstmt.setString(6, now);
            pstmt.executeUpdate();
        }

        // 2. Initial Sample Expenses for student@college.edu (User ID 2)
        String insertExpenseSql = "INSERT INTO expenses (user_id, amount, category, expense_date, description, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertExpenseSql)) {
            Object[][] sampleExpenses = {
                    {2, 120.50, "Education", LocalDate.now().minusDays(5).toString(), "CS Textbooks & Reference Materials", now},
                    {2, 45.00, "Food & Dining", LocalDate.now().minusDays(3).toString(), "Weekly Grocery Shopping", now},
                    {2, 18.75, "Food & Dining", LocalDate.now().minusDays(2).toString(), "Campus Cafeteria Lunch", now},
                    {2, 50.00, "Transportation", LocalDate.now().minusDays(8).toString(), "Monthly Public Bus Pass", now},
                    {2, 29.99, "Entertainment", LocalDate.now().minusDays(1).toString(), "Music & Video Streaming Subscriptions", now},
                    {2, 35.00, "Utilities", LocalDate.now().minusDays(12).toString(), "High-speed Internet Bill share", now}
            };
            for (Object[] exp : sampleExpenses) {
                pstmt.setInt(1, (Integer) exp[0]);
                pstmt.setDouble(2, (Double) exp[1]);
                pstmt.setString(3, (String) exp[2]);
                pstmt.setString(4, (String) exp[3]);
                pstmt.setString(5, (String) exp[4]);
                pstmt.setString(6, (String) exp[5]);
                pstmt.executeUpdate();
            }
        }

        // 3. Initial Budgets for student@college.edu
        String insertBudgetSql = "INSERT INTO budgets (user_id, category, amount_limit, duration, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertBudgetSql)) {
            Object[][] sampleBudgets = {
                    {2, "Food & Dining", 200.00, "Monthly", now},
                    {2, "Transportation", 80.00, "Monthly", now},
                    {2, "Education", 250.00, "Monthly", now},
                    {2, "Entertainment", 60.00, "Monthly", now}
            };
            for (Object[] b : sampleBudgets) {
                pstmt.setInt(1, (Integer) b[0]);
                pstmt.setString(2, (String) b[1]);
                pstmt.setDouble(3, (Double) b[2]);
                pstmt.setString(4, (String) b[3]);
                pstmt.setString(5, (String) b[4]);
                pstmt.executeUpdate();
            }
        }

        // 4. Initial Financial Goals for student@college.edu
        String insertGoalSql = "INSERT INTO goals (user_id, description, target_amount, current_amount, deadline, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertGoalSql)) {
            Object[][] sampleGoals = {
                    {2, "Emergency Savings Fund", 1000.00, 450.00, LocalDate.now().plusMonths(6).toString(), "IN_PROGRESS", now},
                    {2, "New Study Laptop", 750.00, 750.00, LocalDate.now().plusMonths(1).toString(), "COMPLETED", now},
                    {2, "Summer Internship Travel Fund", 500.00, 150.00, LocalDate.now().plusMonths(4).toString(), "IN_PROGRESS", now}
            };
            for (Object[] g : sampleGoals) {
                pstmt.setInt(1, (Integer) g[0]);
                pstmt.setString(2, (String) g[1]);
                pstmt.setDouble(3, (Double) g[2]);
                pstmt.setDouble(4, (Double) g[3]);
                pstmt.setString(5, (String) g[4]);
                pstmt.setString(6, (String) g[5]);
                pstmt.setString(7, (String) g[6]);
                pstmt.executeUpdate();
            }
        }

        // 5. Initial Security Settings
        String insertSecSql = "INSERT INTO security_settings (encryption_standard, tls_required, session_timeout_minutes, mfa_enforcement, lockout_threshold, last_updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSecSql)) {
            pstmt.setString(1, "AES-256 (Simulated Academic Mode)");
            pstmt.setInt(2, 1);
            pstmt.setInt(3, 30);
            pstmt.setString(4, "OPTIONAL");
            pstmt.setInt(5, 5);
            pstmt.setString(6, "admin@finance.edu");
            pstmt.setString(7, now);
            pstmt.executeUpdate();
        }

        // 6. Initial System Settings
        String insertSysSql = "INSERT INTO system_settings (platform_name, default_currency, maintenance_mode, budget_alert_threshold, last_updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSysSql)) {
            pstmt.setString(1, "CampusFin Personal Finance Platform");
            pstmt.setString(2, "USD ($)");
            pstmt.setInt(3, 0);
            pstmt.setInt(4, 80);
            pstmt.setString(5, "admin@finance.edu");
            pstmt.setString(6, now);
            pstmt.executeUpdate();
        }

        // 7. Initial Audit Logs
        String insertLogSql = "INSERT INTO audit_logs (log_type, action, performed_by, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertLogSql)) {
            Object[][] sampleLogs = {
                    {"SYSTEM", "SYSTEM_INIT", "SYSTEM", "College Finance Platform schema and seed data initialized", "127.0.0.1", now},
                    {"SECURITY", "SECURITY_BASELINE", "admin@finance.edu", "Baseline security policy established (AES-256 demo, TLS active, 30m timeout)", "127.0.0.1", now}
            };
            for (Object[] l : sampleLogs) {
                pstmt.setString(1, (String) l[0]);
                pstmt.setString(2, (String) l[1]);
                pstmt.setString(3, (String) l[2]);
                pstmt.setString(4, (String) l[3]);
                pstmt.setString(5, (String) l[4]);
                pstmt.setString(6, (String) l[5]);
                pstmt.executeUpdate();
            }
        }
    }
}

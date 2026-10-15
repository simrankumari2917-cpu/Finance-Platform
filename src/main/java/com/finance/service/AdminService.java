package com.finance.service;

import com.finance.dao.ExpenseDAO;
import com.finance.dao.SettingsDAO;
import com.finance.dao.UserDAO;
import com.finance.model.AuditLog;
import com.finance.model.SecuritySetting;
import com.finance.model.SystemSetting;
import com.finance.model.User;
import com.finance.util.DatabaseManager;
import com.finance.util.PasswordUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing administrative functions: user management, security controls, system configuration, and audit logs.
 */
public class AdminService {

    private final UserDAO userDAO = new UserDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();

    // ==========================================
    // 1. USER MANAGEMENT
    // ==========================================

    public List<User> searchUsers(String query, String role, String status) throws SQLException {
        return userDAO.searchUsers(query, role, status);
    }

    public User createUser(String fullName, String email, String password, String role, String status, String adminEmail) throws Exception {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (userDAO.findByEmail(email.trim()) != null) {
            throw new IllegalArgumentException("A user with email " + email + " already exists.");
        }

        String assignedRole = "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
        String assignedStatus = "SUSPENDED".equalsIgnoreCase(status) ? "SUSPENDED" : "ACTIVE";

        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(PasswordUtil.hashPassword(password));
        u.setRole(assignedRole);
        u.setStatus(assignedStatus);

        User created = userDAO.createUser(u);

        // Audit log
        AuditLog log = new AuditLog();
        log.setLogType("SYSTEM");
        log.setAction("CREATE_USER");
        log.setPerformedBy(adminEmail);
        log.setDetails("Created user account: " + created.getEmail() + " [Role: " + assignedRole + ", Status: " + assignedStatus + "]");
        settingsDAO.addAuditLog(log);

        return created;
    }

    public boolean updateUser(int userId, String fullName, String email, String role, String status, String newPassword, String adminEmail) throws Exception {
        User existing = userDAO.findById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("User not found.");
        }

        if (fullName != null && !fullName.trim().isEmpty()) {
            existing.setFullName(fullName.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            User other = userDAO.findByEmail(email.trim());
            if (other != null && other.getId() != userId) {
                throw new IllegalArgumentException("Email " + email + " is already in use by another account.");
            }
            existing.setEmail(email.trim().toLowerCase());
        }
        if (role != null && !role.trim().isEmpty()) {
            existing.setRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        }
        if (status != null && !status.trim().isEmpty()) {
            existing.setStatus("SUSPENDED".equalsIgnoreCase(status) ? "SUSPENDED" : "ACTIVE");
        }
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
            existing.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        }

        boolean ok = userDAO.updateUser(existing);
        if (ok) {
            AuditLog log = new AuditLog();
            log.setLogType("SYSTEM");
            log.setAction("UPDATE_USER");
            log.setPerformedBy(adminEmail);
            log.setDetails("Updated account id=" + userId + " (" + existing.getEmail() + ")");
            settingsDAO.addAuditLog(log);
        }
        return ok;
    }

    public boolean deleteUser(int currentAdminId, int targetUserId, String adminEmail) throws Exception {
        if (currentAdminId == targetUserId) {
            throw new IllegalArgumentException("You cannot delete your own administrative account.");
        }

        User target = userDAO.findById(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("Target user does not exist.");
        }

        boolean ok = userDAO.deleteUser(targetUserId);
        if (ok) {
            AuditLog log = new AuditLog();
            log.setLogType("SYSTEM");
            log.setAction("DELETE_USER");
            log.setPerformedBy(adminEmail);
            log.setDetails("Deleted account id=" + targetUserId + " (" + target.getEmail() + ")");
            settingsDAO.addAuditLog(log);
        }
        return ok;
    }

    // ==========================================
    // 2. SECURITY SETTINGS
    // ==========================================

    public SecuritySetting getSecuritySettings() throws SQLException {
        return settingsDAO.getSecuritySettings();
    }

    public boolean updateSecuritySettings(SecuritySetting settings, String adminEmail) throws Exception {
        if (settings.getSessionTimeoutMinutes() <= 0) {
            throw new IllegalArgumentException("Session timeout must be greater than 0 minutes.");
        }
        if (settings.getLockoutThreshold() <= 0) {
            throw new IllegalArgumentException("Account lockout threshold must be greater than 0.");
        }

        settings.setLastUpdatedBy(adminEmail);
        boolean ok = settingsDAO.updateSecuritySettings(settings);

        if (ok) {
            AuditLog log = new AuditLog();
            log.setLogType("SECURITY");
            log.setAction("UPDATE_SECURITY_POLICY");
            log.setPerformedBy(adminEmail);
            log.setDetails("Updated security: Standard=" + settings.getEncryptionStandard() +
                    ", TLS=" + settings.isTlsRequired() +
                    ", MFA=" + settings.getMfaEnforcement() +
                    ", Timeout=" + settings.getSessionTimeoutMinutes() + "m");
            settingsDAO.addAuditLog(log);
        }
        return ok;
    }

    public List<AuditLog> getSecurityLogs(int limit) throws SQLException {
        return settingsDAO.getAuditLogs("SECURITY", limit);
    }

    // ==========================================
    // 3. SYSTEM SETTINGS
    // ==========================================

    public SystemSetting getSystemSettings() throws SQLException {
        return settingsDAO.getSystemSettings();
    }

    public boolean updateSystemSettings(SystemSetting settings, String adminEmail) throws Exception {
        if (settings.getPlatformName() == null || settings.getPlatformName().trim().isEmpty()) {
            throw new IllegalArgumentException("Platform name cannot be empty.");
        }
        if (settings.getBudgetAlertThreshold() <= 0 || settings.getBudgetAlertThreshold() > 100) {
            throw new IllegalArgumentException("Budget alert threshold must be between 1% and 100%.");
        }

        settings.setLastUpdatedBy(adminEmail);
        boolean ok = settingsDAO.updateSystemSettings(settings);

        if (ok) {
            AuditLog log = new AuditLog();
            log.setLogType("SYSTEM");
            log.setAction("UPDATE_SYSTEM_SETTINGS");
            log.setPerformedBy(adminEmail);
            log.setDetails("Updated system configuration: Platform=" + settings.getPlatformName() +
                    ", Currency=" + settings.getDefaultCurrency() +
                    ", AlertThreshold=" + settings.getBudgetAlertThreshold() + "%" +
                    ", Maintenance=" + settings.isMaintenanceMode());
            settingsDAO.addAuditLog(log);
        }
        return ok;
    }

    public List<AuditLog> getSystemLogs(int limit) throws SQLException {
        return settingsDAO.getAuditLogs("SYSTEM", limit);
    }

    // ==========================================
    // 4. OVERVIEW DASHBOARD SUMMARY
    // ==========================================

    public Map<String, Object> getAdminDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        int activeUsers = userDAO.countUsersByStatus("ACTIVE");
        int suspendedUsers = userDAO.countUsersByStatus("SUSPENDED");
        int totalUsers = activeUsers + suspendedUsers;

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("suspendedUsers", suspendedUsers);
        stats.put("databaseEngine", DatabaseManager.getActiveDatabaseType());

        return stats;
    }
}

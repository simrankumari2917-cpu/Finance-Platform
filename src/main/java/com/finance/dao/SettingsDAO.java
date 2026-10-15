package com.finance.dao;

import com.finance.model.AuditLog;
import com.finance.model.SecuritySetting;
import com.finance.model.SystemSetting;
import com.finance.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Security Settings, System Settings, and Audit Activity Logs.
 */
public class SettingsDAO {

    // ==========================================
    // SECURITY SETTINGS
    // ==========================================

    public SecuritySetting getSecuritySettings() throws SQLException {
        String sql = "SELECT * FROM security_settings ORDER BY id ASC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                SecuritySetting s = new SecuritySetting();
                s.setId(rs.getInt("id"));
                s.setEncryptionStandard(rs.getString("encryption_standard"));
                s.setTlsRequired(rs.getInt("tls_required") == 1);
                s.setSessionTimeoutMinutes(rs.getInt("session_timeout_minutes"));
                s.setMfaEnforcement(rs.getString("mfa_enforcement"));
                s.setLockoutThreshold(rs.getInt("lockout_threshold"));
                s.setLastUpdatedBy(rs.getString("last_updated_by"));
                return s;
            }
        }
        return null;
    }

    public boolean updateSecuritySettings(SecuritySetting s) throws SQLException {
        String now = LocalDateTime.now().toString().replace("T", " ");
        String sql = "UPDATE security_settings SET encryption_standard = ?, tls_required = ?, " +
                "session_timeout_minutes = ?, mfa_enforcement = ?, lockout_threshold = ?, last_updated_by = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getEncryptionStandard());
            pstmt.setInt(2, s.isTlsRequired() ? 1 : 0);
            pstmt.setInt(3, s.getSessionTimeoutMinutes());
            pstmt.setString(4, s.getMfaEnforcement());
            pstmt.setInt(5, s.getLockoutThreshold());
            pstmt.setString(6, s.getLastUpdatedBy());
            pstmt.setString(7, now);
            pstmt.setInt(8, s.getId() > 0 ? s.getId() : 1);
            return pstmt.executeUpdate() > 0;
        }
    }

    // ==========================================
    // SYSTEM SETTINGS
    // ==========================================

    public SystemSetting getSystemSettings() throws SQLException {
        String sql = "SELECT * FROM system_settings ORDER BY id ASC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                SystemSetting s = new SystemSetting();
                s.setId(rs.getInt("id"));
                s.setPlatformName(rs.getString("platform_name"));
                s.setDefaultCurrency(rs.getString("default_currency"));
                s.setMaintenanceMode(rs.getInt("maintenance_mode") == 1);
                s.setBudgetAlertThreshold(rs.getInt("budget_alert_threshold"));
                s.setLastUpdatedBy(rs.getString("last_updated_by"));
                return s;
            }
        }
        return null;
    }

    public boolean updateSystemSettings(SystemSetting s) throws SQLException {
        String now = LocalDateTime.now().toString().replace("T", " ");
        String sql = "UPDATE system_settings SET platform_name = ?, default_currency = ?, " +
                "maintenance_mode = ?, budget_alert_threshold = ?, last_updated_by = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getPlatformName());
            pstmt.setString(2, s.getDefaultCurrency());
            pstmt.setInt(3, s.isMaintenanceMode() ? 1 : 0);
            pstmt.setInt(4, s.getBudgetAlertThreshold());
            pstmt.setString(5, s.getLastUpdatedBy());
            pstmt.setString(6, now);
            pstmt.setInt(7, s.getId() > 0 ? s.getId() : 1);
            return pstmt.executeUpdate() > 0;
        }
    }

    // ==========================================
    // AUDIT LOGS
    // ==========================================

    public void addAuditLog(AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_logs (log_type, action, performed_by, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getLogType());
            pstmt.setString(2, log.getAction());
            pstmt.setString(3, log.getPerformedBy());
            pstmt.setString(4, log.getDetails());
            pstmt.setString(5, log.getIpAddress() != null ? log.getIpAddress() : "127.0.0.1");
            pstmt.setString(6, LocalDateTime.now().toString().replace("T", " "));
            pstmt.executeUpdate();
        }
    }

    public List<AuditLog> getAuditLogs(String type, int limit) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs");
        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" WHERE log_type = ?");
        }
        sql.append(" ORDER BY id DESC LIMIT ?");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
                pstmt.setString(idx++, type.trim().toUpperCase());
            }
            pstmt.setInt(idx, limit > 0 ? limit : 50);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setId(rs.getInt("id"));
                    log.setLogType(rs.getString("log_type"));
                    log.setAction(rs.getString("action"));
                    log.setPerformedBy(rs.getString("performed_by"));
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    list.add(log);
                }
            }
        }
        return list;
    }
}

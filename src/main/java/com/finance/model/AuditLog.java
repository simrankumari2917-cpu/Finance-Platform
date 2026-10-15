package com.finance.model;

import java.time.LocalDateTime;

/**
 * Model representing audit and activity logs for security and system configuration changes.
 */
public class AuditLog {
    private int id;
    private String logType;       // "SECURITY" or "SYSTEM"
    private String action;        // e.g. "UPDATE_SECURITY_SETTINGS", "UPDATE_SYSTEM_SETTINGS", "DELETE_USER"
    private String performedBy;   // Admin email or username
    private String details;       // Description of modification
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(int id, String logType, String action, String performedBy, String details, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.logType = logType;
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

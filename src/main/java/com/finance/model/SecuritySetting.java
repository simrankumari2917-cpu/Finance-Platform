package com.finance.model;

import java.time.LocalDateTime;

/**
 * Model representing financial data security settings configured by an administrator.
 * Note: These options simulate policy configuration in a collegiate demonstration environment.
 */
public class SecuritySetting {
    private int id;
    private String encryptionStandard; // e.g. "AES-256 (Simulated)", "ChaCha20"
    private boolean tlsRequired;
    private int sessionTimeoutMinutes;
    private String mfaEnforcement;      // "DISABLED", "OPTIONAL", "ENFORCED"
    private int lockoutThreshold;      // failed login attempts
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;

    public SecuritySetting() {}

    public SecuritySetting(int id, String encryptionStandard, boolean tlsRequired, int sessionTimeoutMinutes, 
                           String mfaEnforcement, int lockoutThreshold, String lastUpdatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.encryptionStandard = encryptionStandard;
        this.tlsRequired = tlsRequired;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.mfaEnforcement = mfaEnforcement;
        this.lockoutThreshold = lockoutThreshold;
        this.lastUpdatedBy = lastUpdatedBy;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEncryptionStandard() {
        return encryptionStandard;
    }

    public void setEncryptionStandard(String encryptionStandard) {
        this.encryptionStandard = encryptionStandard;
    }

    public boolean isTlsRequired() {
        return tlsRequired;
    }

    public void setTlsRequired(boolean tlsRequired) {
        this.tlsRequired = tlsRequired;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public String getMfaEnforcement() {
        return mfaEnforcement;
    }

    public void setMfaEnforcement(String mfaEnforcement) {
        this.mfaEnforcement = mfaEnforcement;
    }

    public int getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(int lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

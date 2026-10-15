package com.finance.model;

import java.time.LocalDateTime;

/**
 * Model representing platform-wide system settings and feature toggles.
 */
public class SystemSetting {
    private int id;
    private String platformName;
    private String defaultCurrency;      // e.g. "USD ($)", "EUR (€)", "INR (₹)", "GBP (£)"
    private boolean maintenanceMode;
    private int budgetAlertThreshold;     // e.g. 80%
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;

    public SystemSetting() {}

    public SystemSetting(int id, String platformName, String defaultCurrency, boolean maintenanceMode, 
                         int budgetAlertThreshold, String lastUpdatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.platformName = platformName;
        this.defaultCurrency = defaultCurrency;
        this.maintenanceMode = maintenanceMode;
        this.budgetAlertThreshold = budgetAlertThreshold;
        this.lastUpdatedBy = lastUpdatedBy;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public int getBudgetAlertThreshold() {
        return budgetAlertThreshold;
    }

    public void setBudgetAlertThreshold(int budgetAlertThreshold) {
        this.budgetAlertThreshold = budgetAlertThreshold;
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

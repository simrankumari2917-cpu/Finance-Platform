package com.finance.model;

import java.time.LocalDateTime;

/**
 * Represents a spending budget defined for a specific category and duration.
 * Includes calculated fields for spending utilization and remaining balance.
 */
public class Budget {
    private int id;
    private int userId;
    private String category;
    private double amountLimit;
    private String duration; // e.g. "Monthly", "Weekly", "Yearly"
    private LocalDateTime createdAt;

    // Computed fields dynamically calculated against expenses
    private double spentAmount;
    private double remainingAmount;
    private double utilizationPercentage;

    public Budget() {}

    public Budget(int id, int userId, String category, double amountLimit, String duration, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.amountLimit = amountLimit;
        this.duration = duration;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(double amountLimit) {
        this.amountLimit = amountLimit;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }
}

package com.finance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a savings or financial target set by the user.
 * Tracks current progress towards the target amount and deadline.
 */
public class Goal {
    private int id;
    private int userId;
    private String description;
    private double targetAmount;
    private double currentAmount;
    private LocalDate deadline;
    private String status; // "IN_PROGRESS" or "COMPLETED"
    private LocalDateTime createdAt;

    // Computed fields
    private double progressPercentage;
    private double remainingAmount;

    public Goal() {}

    public Goal(int id, int userId, String description, double targetAmount, double currentAmount, LocalDate deadline, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.status = status;
        this.createdAt = createdAt;
        recalculateProgress();
    }

    public void recalculateProgress() {
        if (targetAmount > 0) {
            this.progressPercentage = Math.min(100.0, Math.round((currentAmount / targetAmount) * 1000.0) / 10.0);
            this.remainingAmount = Math.max(0.0, Math.round((targetAmount - currentAmount) * 100.0) / 100.0);
        } else {
            this.progressPercentage = 0.0;
            this.remainingAmount = 0.0;
        }
        if (currentAmount >= targetAmount && targetAmount > 0) {
            this.status = "COMPLETED";
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
        recalculateProgress();
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
        recalculateProgress();
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
}

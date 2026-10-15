package com.finance.service;

import com.finance.dao.GoalDAO;
import com.finance.model.Goal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Service managing user savings targets and financial milestones.
 */
public class GoalService {

    private final GoalDAO goalDAO = new GoalDAO();

    public Goal addGoal(int userId, String description, double targetAmount, double currentAmount, String deadlineStr) throws Exception {
        validateGoalData(description, targetAmount, currentAmount, deadlineStr);

        LocalDate deadline = LocalDate.parse(deadlineStr.trim());
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setDescription(description.trim());
        goal.setTargetAmount(Math.round(targetAmount * 100.0) / 100.0);
        goal.setCurrentAmount(Math.round(currentAmount * 100.0) / 100.0);
        goal.setDeadline(deadline);

        return goalDAO.createGoal(goal);
    }

    public boolean updateGoal(int userId, int goalId, String description, double targetAmount, double currentAmount, String deadlineStr) throws Exception {
        validateGoalData(description, targetAmount, currentAmount, deadlineStr);

        Goal existing = goalDAO.findById(goalId);
        if (existing == null || existing.getUserId() != userId) {
            throw new IllegalArgumentException("Financial goal not found or access denied.");
        }

        existing.setDescription(description.trim());
        existing.setTargetAmount(Math.round(targetAmount * 100.0) / 100.0);
        existing.setCurrentAmount(Math.round(currentAmount * 100.0) / 100.0);
        existing.setDeadline(LocalDate.parse(deadlineStr.trim()));
        existing.recalculateProgress();

        return goalDAO.updateGoal(existing);
    }

    public boolean addFundsToGoal(int userId, int goalId, double depositAmount) throws Exception {
        if (depositAmount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        return goalDAO.addProgress(goalId, userId, Math.round(depositAmount * 100.0) / 100.0);
    }

    public boolean deleteGoal(int userId, int goalId) throws SQLException {
        return goalDAO.deleteGoal(goalId, userId);
    }

    public List<Goal> getUserGoals(int userId) throws SQLException {
        return goalDAO.getUserGoals(userId);
    }

    private void validateGoalData(String description, double targetAmount, double currentAmount, String deadlineStr) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal description is required.");
        }
        if (targetAmount <= 0) {
            throw new IllegalArgumentException("Target amount must be greater than zero.");
        }
        if (currentAmount < 0) {
            throw new IllegalArgumentException("Current saved amount cannot be negative.");
        }
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Deadline date is required.");
        }
        try {
            LocalDate.parse(deadlineStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid deadline format. Expected YYYY-MM-DD.");
        }
    }
}

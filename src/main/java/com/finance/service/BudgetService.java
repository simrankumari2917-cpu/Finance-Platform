package com.finance.service;

import com.finance.dao.BudgetDAO;
import com.finance.model.Budget;

import java.sql.SQLException;
import java.util.List;

/**
 * Service managing user budget planning and spending utilization analysis.
 */
public class BudgetService {

    private final BudgetDAO budgetDAO = new BudgetDAO();

    public Budget addBudget(int userId, String category, double amountLimit, String duration) throws Exception {
        validateBudgetData(category, amountLimit, duration);

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategory(category.trim());
        budget.setAmountLimit(Math.round(amountLimit * 100.0) / 100.0);
        budget.setDuration(duration.trim());

        return budgetDAO.createBudget(budget);
    }

    public boolean updateBudget(int userId, int budgetId, String category, double amountLimit, String duration) throws Exception {
        validateBudgetData(category, amountLimit, duration);

        Budget existing = budgetDAO.findById(budgetId);
        if (existing == null || existing.getUserId() != userId) {
            throw new IllegalArgumentException("Budget not found or access denied.");
        }

        existing.setCategory(category.trim());
        existing.setAmountLimit(Math.round(amountLimit * 100.0) / 100.0);
        existing.setDuration(duration.trim());

        return budgetDAO.updateBudget(existing);
    }

    public boolean deleteBudget(int userId, int budgetId) throws SQLException {
        return budgetDAO.deleteBudget(budgetId, userId);
    }

    public List<Budget> getUserBudgets(int userId) throws SQLException {
        return budgetDAO.getUserBudgetsWithUtilization(userId);
    }

    private void validateBudgetData(String category, double amountLimit, String duration) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget category is required.");
        }
        if (amountLimit <= 0) {
            throw new IllegalArgumentException("Budget limit must be greater than zero.");
        }
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget duration is required (e.g., Monthly, Weekly).");
        }
    }
}

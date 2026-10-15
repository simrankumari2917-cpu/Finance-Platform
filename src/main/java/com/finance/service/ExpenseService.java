package com.finance.service;

import com.finance.dao.ExpenseDAO;
import com.finance.model.Expense;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service managing user expense operations, validation, and analytics.
 */
public class ExpenseService {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();

    public Expense addExpense(int userId, double amount, String category, String dateStr, String description) throws Exception {
        validateExpenseData(amount, category, dateStr);

        LocalDate date = LocalDate.parse(dateStr.trim());
        Expense exp = new Expense();
        exp.setUserId(userId);
        exp.setAmount(Math.round(amount * 100.0) / 100.0);
        exp.setCategory(category.trim());
        exp.setExpenseDate(date);
        exp.setDescription(description != null ? description.trim() : "");

        return expenseDAO.createExpense(exp);
    }

    public boolean updateExpense(int userId, int expenseId, double amount, String category, String dateStr, String description) throws Exception {
        validateExpenseData(amount, category, dateStr);

        Expense existing = expenseDAO.findById(expenseId);
        if (existing == null || existing.getUserId() != userId) {
            throw new IllegalArgumentException("Expense record not found or access denied.");
        }

        existing.setAmount(Math.round(amount * 100.0) / 100.0);
        existing.setCategory(category.trim());
        existing.setExpenseDate(LocalDate.parse(dateStr.trim()));
        existing.setDescription(description != null ? description.trim() : "");

        return expenseDAO.updateExpense(existing);
    }

    public boolean deleteExpense(int userId, int expenseId) throws SQLException {
        return expenseDAO.deleteExpense(expenseId, userId);
    }

    public List<Expense> getExpenses(int userId, String category, String startDate, String endDate) throws SQLException {
        return expenseDAO.getFilteredExpenses(userId, category, startDate, endDate);
    }

    public Map<String, Object> getExpenseSummary(int userId) throws SQLException {
        Map<String, Object> summary = new HashMap<>();
        double totalSpending = expenseDAO.getTotalSpending(userId);
        double monthlySpending = expenseDAO.getCurrentMonthSpending(userId);
        Map<String, Double> categoryDistribution = expenseDAO.getCategorySpendingSummary(userId);

        summary.put("totalSpending", Math.round(totalSpending * 100.0) / 100.0);
        summary.put("monthlySpending", Math.round(monthlySpending * 100.0) / 100.0);
        summary.put("categoryDistribution", categoryDistribution);

        return summary;
    }

    private void validateExpenseData(double amount, String category, String dateStr) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense date is required.");
        }
        try {
            LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD.");
        }
    }
}

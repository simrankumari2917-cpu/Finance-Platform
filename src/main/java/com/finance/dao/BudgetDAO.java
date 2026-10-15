package com.finance.dao;

import com.finance.model.Budget;
import com.finance.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Budgets.
 * Dynamically computes utilization and remaining balances against recorded expenses.
 */
public class BudgetDAO {

    public Budget createBudget(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (user_id, category, amount_limit, duration, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, budget.getUserId());
            pstmt.setString(2, budget.getCategory());
            pstmt.setDouble(3, budget.getAmountLimit());
            pstmt.setString(4, budget.getDuration());
            pstmt.setString(5, LocalDateTime.now().toString().replace("T", " "));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    budget.setId(rs.getInt(1));
                }
            }
            return budget;
        }
    }

    public boolean updateBudget(Budget budget) throws SQLException {
        String sql = "UPDATE budgets SET category = ?, amount_limit = ?, duration = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, budget.getCategory());
            pstmt.setDouble(2, budget.getAmountLimit());
            pstmt.setString(3, budget.getDuration());
            pstmt.setInt(4, budget.getId());
            pstmt.setInt(5, budget.getUserId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteBudget(int budgetId, int userId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, budgetId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public Budget findById(int budgetId) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, budgetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Budget b = new Budget();
                    b.setId(rs.getInt("id"));
                    b.setUserId(rs.getInt("user_id"));
                    b.setCategory(rs.getString("category"));
                    b.setAmountLimit(rs.getDouble("amount_limit"));
                    b.setDuration(rs.getString("duration"));
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all budgets for a user and calculates utilization against matching expenses.
     */
    public List<Budget> getUserBudgetsWithUtilization(int userId) throws SQLException {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budgets WHERE user_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Budget b = new Budget();
                    b.setId(rs.getInt("id"));
                    b.setUserId(rs.getInt("user_id"));
                    b.setCategory(rs.getString("category"));
                    b.setAmountLimit(rs.getDouble("amount_limit"));
                    b.setDuration(rs.getString("duration"));
                    budgets.add(b);
                }
            }
        }

        // For each budget, compute spent amount from expenses
        for (Budget b : budgets) {
            double spent = calculateSpentForBudget(b.getUserId(), b.getCategory(), b.getDuration());
            b.setSpentAmount(Math.round(spent * 100.0) / 100.0);
            b.setRemainingAmount(Math.round((b.getAmountLimit() - spent) * 100.0) / 100.0);
            if (b.getAmountLimit() > 0) {
                double pct = (spent / b.getAmountLimit()) * 100.0;
                b.setUtilizationPercentage(Math.round(pct * 10.0) / 10.0);
            } else {
                b.setUtilizationPercentage(0.0);
            }
        }

        return budgets;
    }

    private double calculateSpentForBudget(int userId, String category, String duration) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT SUM(amount) FROM expenses WHERE user_id = ? AND category = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(category);

        LocalDate now = LocalDate.now();
        if ("Monthly".equalsIgnoreCase(duration)) {
            String monthPrefix = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
            sql.append(" AND expense_date LIKE ?");
            params.add(monthPrefix + "%");
        } else if ("Weekly".equalsIgnoreCase(duration)) {
            LocalDate weekStart = now.minusDays(7);
            sql.append(" AND expense_date >= ?");
            params.add(weekStart.toString());
        } else if ("Yearly".equalsIgnoreCase(duration)) {
            String yearPrefix = String.format("%04d", now.getYear());
            sql.append(" AND expense_date LIKE ?");
            params.add(yearPrefix + "%");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }
}

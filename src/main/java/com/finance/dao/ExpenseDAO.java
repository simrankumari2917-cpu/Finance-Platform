package com.finance.dao;

import com.finance.model.Expense;
import com.finance.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object for user Expenses.
 * Strictly scopes data queries by user ID for privacy and security.
 */
public class ExpenseDAO {

    public Expense createExpense(Expense expense) throws SQLException {
        String sql = "INSERT INTO expenses (user_id, amount, category, expense_date, description, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, expense.getUserId());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getCategory());
            pstmt.setString(4, expense.getExpenseDate().toString());
            pstmt.setString(5, expense.getDescription() != null ? expense.getDescription() : "");
            pstmt.setString(6, LocalDateTime.now().toString().replace("T", " "));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    expense.setId(rs.getInt(1));
                }
            }
            return expense;
        }
    }

    public boolean updateExpense(Expense expense) throws SQLException {
        String sql = "UPDATE expenses SET amount = ?, category = ?, expense_date = ?, description = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, expense.getAmount());
            pstmt.setString(2, expense.getCategory());
            pstmt.setString(3, expense.getExpenseDate().toString());
            pstmt.setString(4, expense.getDescription() != null ? expense.getDescription() : "");
            pstmt.setInt(5, expense.getId());
            pstmt.setInt(6, expense.getUserId()); // ownership check
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteExpense(int expenseId, int userId) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            pstmt.setInt(2, userId); // ownership check
            return pstmt.executeUpdate() > 0;
        }
    }

    public Expense findById(int expenseId) throws SQLException {
        String sql = "SELECT * FROM expenses WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, expenseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToExpense(rs);
                }
            }
        }
        return null;
    }

    public List<Expense> getFilteredExpenses(int userId, String category, String startDate, String endDate) throws SQLException {
        List<Expense> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM expenses WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND expense_date >= ?");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND expense_date <= ?");
            params.add(endDate.trim());
        }
        sql.append(" ORDER BY expense_date DESC, id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExpense(rs));
                }
            }
        }
        return list;
    }

    public double getTotalSpending(int userId) throws SQLException {
        String sql = "SELECT SUM(amount) FROM expenses WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getCurrentMonthSpending(int userId) throws SQLException {
        LocalDate now = LocalDate.now();
        String prefix = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        String sql = "SELECT SUM(amount) FROM expenses WHERE user_id = ? AND expense_date LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, prefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public Map<String, Double> getCategorySpendingSummary(int userId) throws SQLException {
        Map<String, Double> summary = new LinkedHashMap<>();
        String sql = "SELECT category, SUM(amount) as total FROM expenses WHERE user_id = ? GROUP BY category ORDER BY total DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.put(rs.getString("category"), Math.round(rs.getDouble("total") * 100.0) / 100.0);
                }
            }
        }
        return summary;
    }

    private Expense mapRowToExpense(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getInt("id"));
        e.setUserId(rs.getInt("user_id"));
        e.setAmount(rs.getDouble("amount"));
        e.setCategory(rs.getString("category"));
        e.setExpenseDate(LocalDate.parse(rs.getString("expense_date")));
        e.setDescription(rs.getString("description"));
        return e;
    }
}

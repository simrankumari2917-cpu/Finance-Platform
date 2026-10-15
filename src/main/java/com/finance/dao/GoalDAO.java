package com.finance.dao;

import com.finance.model.Goal;
import com.finance.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Financial Goals.
 * Handles tracking, milestone progress updates, and completion status.
 */
public class GoalDAO {

    public Goal createGoal(Goal goal) throws SQLException {
        String sql = "INSERT INTO goals (user_id, description, target_amount, current_amount, deadline, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, goal.getUserId());
            pstmt.setString(2, goal.getDescription());
            pstmt.setDouble(3, goal.getTargetAmount());
            pstmt.setDouble(4, goal.getCurrentAmount());
            pstmt.setString(5, goal.getDeadline().toString());
            pstmt.setString(6, goal.getCurrentAmount() >= goal.getTargetAmount() ? "COMPLETED" : "IN_PROGRESS");
            pstmt.setString(7, LocalDateTime.now().toString().replace("T", " "));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    goal.setId(rs.getInt(1));
                }
            }
            goal.recalculateProgress();
            return goal;
        }
    }

    public boolean updateGoal(Goal goal) throws SQLException {
        String status = goal.getCurrentAmount() >= goal.getTargetAmount() ? "COMPLETED" : "IN_PROGRESS";
        String sql = "UPDATE goals SET description = ?, target_amount = ?, current_amount = ?, deadline = ?, status = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, goal.getDescription());
            pstmt.setDouble(2, goal.getTargetAmount());
            pstmt.setDouble(3, goal.getCurrentAmount());
            pstmt.setString(4, goal.getDeadline().toString());
            pstmt.setString(5, status);
            pstmt.setInt(6, goal.getId());
            pstmt.setInt(7, goal.getUserId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean addProgress(int goalId, int userId, double addedAmount) throws SQLException {
        Goal existing = findById(goalId);
        if (existing == null || existing.getUserId() != userId) {
            return false;
        }
        double newAmount = Math.max(0.0, existing.getCurrentAmount() + addedAmount);
        existing.setCurrentAmount(newAmount);
        return updateGoal(existing);
    }

    public boolean deleteGoal(int goalId, int userId) throws SQLException {
        String sql = "DELETE FROM goals WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, goalId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public Goal findById(int goalId) throws SQLException {
        String sql = "SELECT * FROM goals WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, goalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGoal(rs);
                }
            }
        }
        return null;
    }

    public List<Goal> getUserGoals(int userId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE user_id = ? ORDER BY status DESC, deadline ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGoal(rs));
                }
            }
        }
        return list;
    }

    private Goal mapRowToGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setUserId(rs.getInt("user_id"));
        g.setDescription(rs.getString("description"));
        g.setTargetAmount(rs.getDouble("target_amount"));
        g.setCurrentAmount(rs.getDouble("current_amount"));
        g.setDeadline(LocalDate.parse(rs.getString("deadline")));
        g.setStatus(rs.getString("status"));
        g.recalculateProgress();
        return g;
    }
}

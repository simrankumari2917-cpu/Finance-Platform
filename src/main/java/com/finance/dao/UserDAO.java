package com.finance.dao;

import com.finance.model.User;
import com.finance.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User accounts.
 * Provides CRUD, searching, and filtering operations for user management.
 */
public class UserDAO {

    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail().trim().toLowerCase());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getStatus());
            pstmt.setString(6, LocalDateTime.now().toString().replace("T", " "));
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            return user;
        }
    }

    public boolean updateUser(User user) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE users SET full_name = ?, email = ?, role = ?, status = ?");
        boolean updatePass = user.getPasswordHash() != null && !user.getPasswordHash().isEmpty();
        if (updatePass) {
            sql.append(", password_hash = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail().trim().toLowerCase());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getStatus());
            int idx = 5;
            if (updatePass) {
                pstmt.setString(idx++, user.getPasswordHash());
            }
            pstmt.setInt(idx, user.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        // First delete associated user expenses, budgets, goals for cascade safety
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement p1 = conn.prepareStatement("DELETE FROM expenses WHERE user_id = ?")) {
                p1.setInt(1, userId);
                p1.executeUpdate();
            }
            try (PreparedStatement p2 = conn.prepareStatement("DELETE FROM budgets WHERE user_id = ?")) {
                p2.setInt(1, userId);
                p2.executeUpdate();
            }
            try (PreparedStatement p3 = conn.prepareStatement("DELETE FROM goals WHERE user_id = ?")) {
                p3.setInt(1, userId);
                p3.executeUpdate();
            }
            try (PreparedStatement p4 = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                p4.setInt(1, userId);
                return p4.executeUpdate() > 0;
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        return searchUsers(null, null, null);
    }

    public List<User> searchUsers(String query, String roleFilter, String statusFilter) throws SQLException {
        List<User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?)");
            String qPattern = "%" + query.trim().toLowerCase() + "%";
            params.add(qPattern);
            params.add(qPattern);
        }
        if (roleFilter != null && !roleFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleFilter)) {
            sql.append(" AND role = ?");
            params.add(roleFilter.trim().toUpperCase());
        }
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND status = ?");
            params.add(statusFilter.trim().toUpperCase());
        }
        sql.append(" ORDER BY id ASC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
        }
        return list;
    }

    public int countUsersByStatus(String status) throws SQLException {
        String sql = status == null ? "SELECT COUNT(*) FROM users" : "SELECT COUNT(*) FROM users WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (status != null) {
                pstmt.setString(1, status.toUpperCase());
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        return u;
    }
}

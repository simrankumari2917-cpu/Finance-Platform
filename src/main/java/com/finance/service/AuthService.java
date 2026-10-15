package com.finance.service;

import com.finance.dao.UserDAO;
import com.finance.model.User;
import com.finance.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user authentication, registration, password hashing, and active sessions.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    // In-memory token to User mapping for simple session tracking
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    public static class UserSession {
        private final User user;
        private final long loginTime;

        public UserSession(User user) {
            this.user = user;
            this.loginTime = System.currentTimeMillis();
        }

        public User getUser() {
            return user;
        }

        public long getLoginTime() {
            return loginTime;
        }
    }

    /**
     * Authenticates a user with email and plain-text password.
     */
    public String login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Your account has been suspended. Please contact an administrator.");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new UserSession(user));
        return token;
    }

    /**
     * Registers a new regular user account.
     */
    public User register(String fullName, String email, String password) throws Exception {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Valid email address is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (userDAO.findByEmail(email.trim()) != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPasswordHash(PasswordUtil.hashPassword(password));
        newUser.setRole("USER");
        newUser.setStatus("ACTIVE");

        return userDAO.createUser(newUser);
    }

    public User getAuthenticatedUser(String token) {
        if (token == null) return null;
        UserSession session = activeSessions.get(token);
        if (session != null) {
            // Refresh latest user status from database to reflect any admin changes (e.g. suspension)
            try {
                User fresh = userDAO.findById(session.getUser().getId());
                if (fresh != null && fresh.isActive()) {
                    return fresh;
                }
            } catch (SQLException e) {
                return session.getUser();
            }
        }
        return null;
    }

    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }
}

package com.finance.web;

import com.finance.model.*;
import com.finance.service.*;
import com.finance.util.DatabaseManager;
import com.finance.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Built-in HTTP Server handling static web assets and RESTful API endpoints.
 * Implements strict role-based authorization and user data isolation.
 */
public class HttpServerApp {

    private static final AuthService authService = new AuthService();
    private static final ExpenseService expenseService = new ExpenseService();
    private static final BudgetService budgetService = new BudgetService();
    private static final GoalService goalService = new GoalService();
    private static final AdminService adminService = new AdminService();

    public static void main(String[] args) {
        int port = 8080;
        try {
            // 1. Initialize Database & Seed Sample Data
            DatabaseManager.initializeDatabase();

            // 2. Start HTTP Server
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoints
            server.createContext("/api/", new ApiHandler());

            // Static Frontend Assets
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("==================================================================");
            System.out.println("  Personal Finance Management Platform is RUNNING!");
            System.out.println("  Access the Web Interface at: http://localhost:" + port);
            System.out.println("  Database Engine Active: " + DatabaseManager.getActiveDatabaseType());
            System.out.println("  Default Admin Login: admin@finance.edu / Admin@123");
            System.out.println("  Default User Login:  student@college.edu / User@123");
            System.out.println("==================================================================");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================
    // API ROUTER & CONTROLLER
    // ==================================================================

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS for local development
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // Public endpoints
                if ("/api/public/config".equals(path) && "GET".equals(method)) {
                    handlePublicConfig(exchange);
                    return;
                }
                if ("/api/auth/login".equals(path) && "POST".equals(method)) {
                    handleLogin(exchange);
                    return;
                }
                if ("/api/auth/register".equals(path) && "POST".equals(method)) {
                    handleRegister(exchange);
                    return;
                }

                // Authenticated endpoints require valid session token
                User currentUser = authenticate(exchange);
                if (currentUser == null) {
                    sendError(exchange, 401, "Unauthorized: Please log in to continue.");
                    return;
                }

                if ("/api/auth/me".equals(path) && "GET".equals(method)) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", currentUser.getId());
                    userData.put("fullName", currentUser.getFullName());
                    userData.put("email", currentUser.getEmail());
                    userData.put("role", currentUser.getRole());
                    userData.put("status", currentUser.getStatus());
                    sendSuccess(exchange, "Current user session", userData);
                    return;
                }

                if ("/api/auth/logout".equals(path) && "POST".equals(method)) {
                    String token = extractToken(exchange);
                    authService.logout(token);
                    sendSuccess(exchange, "Logged out successfully.", null);
                    return;
                }

                // User-scoped Finance Endpoints
                if (path.startsWith("/api/expenses")) {
                    handleExpenses(exchange, currentUser, path, method);
                    return;
                }
                if (path.startsWith("/api/budgets")) {
                    handleBudgets(exchange, currentUser, path, method);
                    return;
                }
                if (path.startsWith("/api/goals")) {
                    handleGoals(exchange, currentUser, path, method);
                    return;
                }

                // Admin-scoped Endpoints (Strictly require ADMIN role)
                if (path.startsWith("/api/admin")) {
                    if (!currentUser.isAdmin()) {
                        sendError(exchange, 403, "Access Denied: Administrative privileges required.");
                        return;
                    }
                    handleAdmin(exchange, currentUser, path, method);
                    return;
                }

                sendError(exchange, 404, "API endpoint not found: " + path);

            } catch (IllegalArgumentException | IllegalStateException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Server error: " + e.getMessage());
            }
        }

        private void handlePublicConfig(HttpExchange exchange) throws Exception {
            SystemSetting sys = adminService.getSystemSettings();
            Map<String, Object> res = new HashMap<>();
            res.put("platformName", sys != null ? sys.getPlatformName() : "CampusFin");
            res.put("defaultCurrency", sys != null ? sys.getDefaultCurrency() : "USD ($)");
            res.put("maintenanceMode", sys != null && sys.isMaintenanceMode());
            sendSuccess(exchange, "Config loaded", res);
        }

        private void handleLogin(HttpExchange exchange) throws Exception {
            Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            String token = authService.login(email, password);
            User user = authService.getAuthenticatedUser(token);

            Map<String, Object> res = new HashMap<>();
            res.put("token", token);
            res.put("id", user.getId());
            res.put("fullName", user.getFullName());
            res.put("email", user.getEmail());
            res.put("role", user.getRole());

            sendSuccess(exchange, "Login successful. Welcome back, " + user.getFullName() + "!", res);
        }

        private void handleRegister(HttpExchange exchange) throws Exception {
            Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
            String fullName = (String) body.get("fullName");
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            User created = authService.register(fullName, email, password);
            String token = authService.login(email, password);

            Map<String, Object> res = new HashMap<>();
            res.put("token", token);
            res.put("id", created.getId());
            res.put("fullName", created.getFullName());
            res.put("email", created.getEmail());
            res.put("role", created.getRole());

            sendSuccess(exchange, "Account created successfully!", res);
        }

        // ==============================================================
        // EXPENSES CONTROLLER
        // ==============================================================
        private void handleExpenses(HttpExchange exchange, User user, String path, String method) throws Exception {
            if ("/api/expenses".equals(path) && "GET".equals(method)) {
                Map<String, String> query = parseQuery(exchange);
                String category = query.get("category");
                String startDate = query.get("startDate");
                String endDate = query.get("endDate");
                List<Expense> expenses = expenseService.getExpenses(user.getId(), category, startDate, endDate);
                sendSuccess(exchange, "Expenses retrieved", expenses);
            } else if ("/api/expenses".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                double amount = parseDouble(body.get("amount"));
                String category = (String) body.get("category");
                String date = (String) body.get("expenseDate");
                String desc = (String) body.get("description");

                Expense exp = expenseService.addExpense(user.getId(), amount, category, date, desc);
                sendSuccess(exchange, "Expense added successfully!", exp);
            } else if ("/api/expenses/update".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                double amount = parseDouble(body.get("amount"));
                String category = (String) body.get("category");
                String date = (String) body.get("expenseDate");
                String desc = (String) body.get("description");

                boolean ok = expenseService.updateExpense(user.getId(), id, amount, category, date, desc);
                if (ok) {
                    sendSuccess(exchange, "Expense updated successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not update expense.");
                }
            } else if ("/api/expenses/delete".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                boolean ok = expenseService.deleteExpense(user.getId(), id);
                if (ok) {
                    sendSuccess(exchange, "Expense deleted successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not delete expense.");
                }
            } else if ("/api/expenses/summary".equals(path) && "GET".equals(method)) {
                Map<String, Object> summary = expenseService.getExpenseSummary(user.getId());
                sendSuccess(exchange, "Expense summary retrieved", summary);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        }

        // ==============================================================
        // BUDGETS CONTROLLER
        // ==============================================================
        private void handleBudgets(HttpExchange exchange, User user, String path, String method) throws Exception {
            if ("/api/budgets".equals(path) && "GET".equals(method)) {
                List<Budget> budgets = budgetService.getUserBudgets(user.getId());
                sendSuccess(exchange, "Budgets retrieved", budgets);
            } else if ("/api/budgets".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String category = (String) body.get("category");
                double limit = parseDouble(body.get("amountLimit"));
                String duration = (String) body.get("duration");

                Budget b = budgetService.addBudget(user.getId(), category, limit, duration);
                sendSuccess(exchange, "Budget created successfully!", b);
            } else if ("/api/budgets/update".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                String category = (String) body.get("category");
                double limit = parseDouble(body.get("amountLimit"));
                String duration = (String) body.get("duration");

                boolean ok = budgetService.updateBudget(user.getId(), id, category, limit, duration);
                if (ok) {
                    sendSuccess(exchange, "Budget updated successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not update budget.");
                }
            } else if ("/api/budgets/delete".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                boolean ok = budgetService.deleteBudget(user.getId(), id);
                if (ok) {
                    sendSuccess(exchange, "Budget deleted successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not delete budget.");
                }
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        }

        // ==============================================================
        // FINANCIAL GOALS CONTROLLER
        // ==============================================================
        private void handleGoals(HttpExchange exchange, User user, String path, String method) throws Exception {
            if ("/api/goals".equals(path) && "GET".equals(method)) {
                List<Goal> goals = goalService.getUserGoals(user.getId());
                sendSuccess(exchange, "Goals retrieved", goals);
            } else if ("/api/goals".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String desc = (String) body.get("description");
                double target = parseDouble(body.get("targetAmount"));
                double current = parseDouble(body.get("currentAmount"));
                String deadline = (String) body.get("deadline");

                Goal g = goalService.addGoal(user.getId(), desc, target, current, deadline);
                sendSuccess(exchange, "Financial goal created successfully!", g);
            } else if ("/api/goals/update".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                String desc = (String) body.get("description");
                double target = parseDouble(body.get("targetAmount"));
                double current = parseDouble(body.get("currentAmount"));
                String deadline = (String) body.get("deadline");

                boolean ok = goalService.updateGoal(user.getId(), id, desc, target, current, deadline);
                if (ok) {
                    sendSuccess(exchange, "Goal updated successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not update goal.");
                }
            } else if ("/api/goals/deposit".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                double amount = parseDouble(body.get("amount"));

                boolean ok = goalService.addFundsToGoal(user.getId(), id, amount);
                if (ok) {
                    sendSuccess(exchange, "Funds added to goal successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not update goal progress.");
                }
            } else if ("/api/goals/delete".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                boolean ok = goalService.deleteGoal(user.getId(), id);
                if (ok) {
                    sendSuccess(exchange, "Goal deleted successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not delete goal.");
                }
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        }

        // ==============================================================
        // ADMIN CONTROLLER
        // ==============================================================
        private void handleAdmin(HttpExchange exchange, User admin, String path, String method) throws Exception {
            if ("/api/admin/stats".equals(path) && "GET".equals(method)) {
                Map<String, Object> stats = adminService.getAdminDashboardStats();
                sendSuccess(exchange, "Admin statistics", stats);
            } else if ("/api/admin/users".equals(path) && "GET".equals(method)) {
                Map<String, String> query = parseQuery(exchange);
                List<User> users = adminService.searchUsers(query.get("q"), query.get("role"), query.get("status"));
                sendSuccess(exchange, "Users retrieved", users);
            } else if ("/api/admin/users".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String name = (String) body.get("fullName");
                String email = (String) body.get("email");
                String password = (String) body.get("password");
                String role = (String) body.get("role");
                String status = (String) body.get("status");

                User created = adminService.createUser(name, email, password, role, status, admin.getEmail());
                sendSuccess(exchange, "User account created successfully!", created);
            } else if ("/api/admin/users/update".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int id = parseInt(body.get("id"));
                String name = (String) body.get("fullName");
                String email = (String) body.get("email");
                String role = (String) body.get("role");
                String status = (String) body.get("status");
                String newPass = (String) body.get("password");

                boolean ok = adminService.updateUser(id, name, email, role, status, newPass, admin.getEmail());
                if (ok) {
                    sendSuccess(exchange, "User details updated successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not update user account.");
                }
            } else if ("/api/admin/users/delete".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                int targetId = parseInt(body.get("id"));
                boolean ok = adminService.deleteUser(admin.getId(), targetId, admin.getEmail());
                if (ok) {
                    sendSuccess(exchange, "User account deleted successfully!", null);
                } else {
                    sendError(exchange, 400, "Could not delete user account.");
                }
            } else if ("/api/admin/security".equals(path) && "GET".equals(method)) {
                SecuritySetting sec = adminService.getSecuritySettings();
                sendSuccess(exchange, "Security settings retrieved", sec);
            } else if ("/api/admin/security".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                SecuritySetting sec = adminService.getSecuritySettings();
                if (sec == null) sec = new SecuritySetting();
                sec.setEncryptionStandard((String) body.get("encryptionStandard"));
                sec.setTlsRequired(Boolean.TRUE.equals(body.get("tlsRequired")));
                sec.setSessionTimeoutMinutes(parseInt(body.get("sessionTimeoutMinutes")));
                sec.setMfaEnforcement((String) body.get("mfaEnforcement"));
                sec.setLockoutThreshold(parseInt(body.get("lockoutThreshold")));

                boolean ok = adminService.updateSecuritySettings(sec, admin.getEmail());
                if (ok) {
                    sendSuccess(exchange, "Security configuration saved successfully!", sec);
                } else {
                    sendError(exchange, 400, "Could not update security settings.");
                }
            } else if ("/api/admin/security/logs".equals(path) && "GET".equals(method)) {
                List<AuditLog> logs = adminService.getSecurityLogs(50);
                sendSuccess(exchange, "Security activity logs retrieved", logs);
            } else if ("/api/admin/system".equals(path) && "GET".equals(method)) {
                SystemSetting sys = adminService.getSystemSettings();
                sendSuccess(exchange, "System settings retrieved", sys);
            } else if ("/api/admin/system".equals(path) && "POST".equals(method)) {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                SystemSetting sys = adminService.getSystemSettings();
                if (sys == null) sys = new SystemSetting();
                sys.setPlatformName((String) body.get("platformName"));
                sys.setDefaultCurrency((String) body.get("defaultCurrency"));
                sys.setMaintenanceMode(Boolean.TRUE.equals(body.get("maintenanceMode")));
                sys.setBudgetAlertThreshold(parseInt(body.get("budgetAlertThreshold")));

                boolean ok = adminService.updateSystemSettings(sys, admin.getEmail());
                if (ok) {
                    sendSuccess(exchange, "System configuration updated successfully!", sys);
                } else {
                    sendError(exchange, 400, "Could not update system settings.");
                }
            } else if ("/api/admin/system/logs".equals(path) && "GET".equals(method)) {
                List<AuditLog> logs = adminService.getSystemLogs(50);
                sendSuccess(exchange, "System configuration logs retrieved", logs);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        }
    }

    // ==================================================================
    // STATIC RESOURCE HANDLER
    // ==================================================================

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Resource path inside classpath or filesystem
            InputStream in = getClass().getResourceAsStream("/static" + path);
            if (in == null) {
                // Try from project directory
                File file = new File("src/main/resources/static" + path);
                if (file.exists() && !file.isDirectory()) {
                    in = new FileInputStream(file);
                }
            }

            if (in == null) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);

            byte[] data = in.readAllBytes();
            in.close();

            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".svg")) return "image/svg+xml";
            return "text/plain";
        }
    }

    // ==================================================================
    // HELPER METHODS
    // ==================================================================

    private static User authenticate(HttpExchange exchange) {
        String token = extractToken(exchange);
        if (token == null || token.isEmpty()) {
            return null;
        }
        return authService.getAuthenticatedUser(token);
    }

    private static String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        Map<String, String> query = parseQuery(exchange);
        return query.get("token");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> map = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) return map;

        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(key, val);
                } else if (!pair.isEmpty()) {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        if (data != null) {
            resp.put("data", data);
        }
        writeJsonResponse(exchange, 200, resp);
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("error", message);
        writeJsonResponse(exchange, status, resp);
    }

    private static void writeJsonResponse(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static int parseInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}

package com.studentmarketplace.service;

import com.studentmarketplace.database.DatabaseManager;
import com.studentmarketplace.model.User;
import com.studentmarketplace.model.UserSession;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles user authentication and role resolution.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final DatabaseManager dbManager;

    public AuthService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Optional<AuthPayload> loginWithUser(String username, String password) {
        String sql = "SELECT user_id, username, email, full_name, student_id, university, phone, profile_image_path, bio, " +
                "rating, total_ratings, password_hash, user_role, account_status, created_at, updated_at " +
                "FROM users WHERE username = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String accountStatus = rs.getString("account_status");
                if (!"ACTIVE".equalsIgnoreCase(accountStatus)) {
                    return Optional.empty();
                }

                String storedHash = rs.getString("password_hash");
                if (!verifyPassword(password, storedHash)) {
                    return Optional.empty();
                }

                if (!isBCryptHash(storedHash)) {
                    upgradeHash(rs.getInt("user_id"), password);
                }

                String roleText = rs.getString("user_role");
                UserSession.Role role = "ADMIN".equalsIgnoreCase(roleText)
                        ? UserSession.Role.ADMIN
                        : UserSession.Role.CLIENT;

                UserSession session = new UserSession(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        role
                );

                User user = mapResultSetToUser(rs);
                return Optional.of(new AuthPayload(user, session));
            }
        } catch (Exception e) {
            logger.error("Login failed for username: {}", username, e);
            return Optional.empty();
        }
    }

    public Optional<UserSession> login(String username, String password) {
        return loginWithUser(username, password).map(AuthPayload::session);
    }

    public Optional<UserSession> register(String username,
                                          String email,
                                          String fullName,
                                          String studentId,
                                          String university,
                                          String password) {
        return registerDetailed(username, email, fullName, studentId, university, password)
                .session()
                .map(AuthPayload::session);
    }

    public RegistrationResult registerDetailed(String username,
                                               String email,
                                               String fullName,
                                               String studentId,
                                               String university,
                                               String password) {
        String cleanUsername = safe(username);
        String cleanEmail = safe(email);
        String cleanFullName = safe(fullName);
        String cleanStudentId = safe(studentId);
        String cleanUniversity = safe(university);

        if (cleanUsername.isEmpty() || cleanEmail.isEmpty() || cleanFullName.isEmpty()
                || cleanStudentId.isEmpty() || cleanUniversity.isEmpty() || password == null || password.length() < 8) {
            return RegistrationResult.failure("Please complete all fields and use a password with at least 8 characters.");
        }

        if (!isUnique("username", cleanUsername)) {
            return RegistrationResult.failure("Username already exists.");
        }
        if (!isUnique("email", cleanEmail)) {
            return RegistrationResult.failure("Email already exists.");
        }
        if (!isUnique("student_id", cleanStudentId)) {
            return RegistrationResult.failure("Student ID already exists.");
        }

        String insertSql = "INSERT INTO users (username, email, password_hash, full_name, student_id, university, user_role) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'CLIENT')";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(insertSql)) {
            String bcryptHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

            stmt.setString(1, cleanUsername);
            stmt.setString(2, cleanEmail);
            stmt.setString(3, bcryptHash);
            stmt.setString(4, cleanFullName);
            stmt.setString(5, cleanStudentId);
            stmt.setString(6, cleanUniversity);
            stmt.executeUpdate();

            int userId = resolveInsertedUserId(cleanUsername);
            if (userId <= 0) {
                return RegistrationResult.failure("Registration failed: account was inserted but could not be loaded.");
            }

            UserSession userSession = new UserSession(userId, cleanUsername, cleanFullName, UserSession.Role.CLIENT);
            User user = createUserFromRegistration(userId, cleanUsername, cleanEmail, cleanFullName, cleanStudentId, cleanUniversity, bcryptHash);
            return RegistrationResult.success(userSession, user);
        } catch (Exception e) {
            logger.error("Registration failed for username: {}", cleanUsername, e);
            String msg = e.getMessage() == null ? "Registration failed." : e.getMessage();
            if (msg.toLowerCase().contains("unique")) {
                return RegistrationResult.failure("Username, email, or student ID already exists.");
            }
            return RegistrationResult.failure("Registration failed due to a database error.");
        }
    }

    private int resolveInsertedUserId(String username) {
        String keySql = "SELECT last_insert_rowid() AS user_id";
        try (PreparedStatement keyStmt = dbManager.getConnection().prepareStatement(keySql);
             ResultSet keys = keyStmt.executeQuery()) {
            if (keys.next() && keys.getInt("user_id") > 0) {
                return keys.getInt("user_id");
            }
        } catch (Exception ignored) {
            // fall through to fetch by username
        }

        String fetchSql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement fetchStmt = dbManager.getConnection().prepareStatement(fetchSql)) {
            fetchStmt.setString(1, username);
            try (ResultSet rs = fetchStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (Exception ignored) {
            // no-op
        }
        return -1;
    }

    private boolean isUnique(String column, String value) {
        String sql = "SELECT COUNT(*) FROM users WHERE " + column + " = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            logger.error("Uniqueness check failed for {}", column, e);
            return false;
        }
    }

    private boolean verifyPassword(String plainPassword, String storedHash) {
        if (storedHash == null || plainPassword == null) {
            return false;
        }

        if (isBCryptHash(storedHash)) {
            return BCrypt.checkpw(plainPassword, storedHash);
        }

        return storedHash.equals(legacyHash(plainPassword));
    }

    private boolean isBCryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private void upgradeHash(int userId, String plainPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        String bcryptHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        dbManager.executeUpdate(sql, bcryptHash, userId);
    }

    private String legacyHash(String password) {
        return Integer.toHexString(password.hashCode());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private User createUserFromRegistration(int userId,
                                            String username,
                                            String email,
                                            String fullName,
                                            String studentId,
                                            String university,
                                            String passwordHash) {
        LocalDateTime now = LocalDateTime.now();
        return new User(
                userId,
                username,
                email,
                passwordHash,
                fullName,
                studentId,
                university,
                null,
                null,
                null,
                5.0,
                0,
                User.AccountStatus.ACTIVE,
                now,
                now
        );
    }

    private User mapResultSetToUser(ResultSet rs) throws java.sql.SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                rs.getString("student_id"),
                rs.getString("university"),
                rs.getString("phone"),
                rs.getString("profile_image_path"),
                rs.getString("bio"),
                rs.getDouble("rating"),
                rs.getInt("total_ratings"),
                User.AccountStatus.valueOf(rs.getString("account_status")),
                parseSqlDateTime(rs.getString("created_at")),
                parseSqlDateTime(rs.getString("updated_at"))
        );
    }

    private LocalDateTime parseSqlDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(value.replace(" ", "T"));
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    public record RegistrationResult(boolean success, String message, Optional<AuthPayload> session) {
        public static RegistrationResult success(UserSession userSession, User user) {
            return new RegistrationResult(true, "Registration successful.", Optional.of(new AuthPayload(user, userSession)));
        }

        public static RegistrationResult failure(String message) {
            return new RegistrationResult(false, message, Optional.empty());
        }
    }

    public record AuthPayload(User user, UserSession session) {
    }
}

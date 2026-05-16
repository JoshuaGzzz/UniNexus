package com.studentmarketplace.model;

/**
 * Immutable authenticated user session context.
 */
public class UserSession {
    private static volatile UserSession currentSession;
    private static volatile User currentUser;

    public enum Role {
        CLIENT,
        ADMIN
    }

    private final int userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public UserSession(int userId, String username, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public static synchronized void setCurrent(User user, UserSession session) {
        currentUser = user;
        currentSession = session;
    }

    public static UserSession getCurrentSession() {
        return currentSession;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void clearCurrent() {
        currentUser = null;
        currentSession = null;
    }
}

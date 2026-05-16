package com.studentmarketplace.util;

import com.studentmarketplace.model.User;
import com.studentmarketplace.model.UserSession;

/**
 * Global authenticated session holder.
 */
public final class SessionManager {
    private static SessionManager instance;

    private User currentUser;
    private UserSession currentSession;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public synchronized void setSession(User user, UserSession session) {
        this.currentUser = user;
        this.currentSession = session;
        UserSession.setCurrent(user, session);
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized UserSession getCurrentSession() {
        return currentSession;
    }

    public synchronized int getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public synchronized void clear() {
        this.currentUser = null;
        this.currentSession = null;
        UserSession.clearCurrent();
    }
}

package com.lendahand.utils;

import com.lendahand.models.User;

/**
 * Singleton that holds the currently authenticated user for the session.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser()               { return currentUser; }
    public void setCurrentUser(User user)      { this.currentUser = user; }

    public boolean isLoggedIn()                { return currentUser != null; }

    /** Returns true when the logged-in user is a TaskPoster. */
    public boolean isPoster() {
        return currentUser != null && "POSTER".equals(currentUser.getUserType());
    }

    /** Returns true when the logged-in user is a Helper. */
    public boolean isHelper() {
        return currentUser != null && "HELPER".equals(currentUser.getUserType());
    }

    public void logout() { currentUser = null; }
}

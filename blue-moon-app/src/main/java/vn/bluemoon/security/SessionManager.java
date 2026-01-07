package vn.bluemoon.security;

import vn.bluemoon.model.entity.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session manager for user sessions
 */
public class SessionManager {
    private static SessionManager instance;
    private Map<String, User> sessions = new HashMap<>();
    private Map<String, Long> sessionTimestamps = new HashMap<>();
    private static final long SESSION_TIMEOUT = 8 * 60 * 60 * 1000; // 8 hours

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Create a new session
     * @param user User to create session for
     * @return Session token
     */
    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        sessionTimestamps.put(token, System.currentTimeMillis());
        return token;
    }

    /**
     * Get user from session token
     * @param token Session token
     * @return User if session is valid, null otherwise
     */
    public User getUser(String token) {
        if (token == null || !sessions.containsKey(token)) {
            return null;
        }
        
        Long timestamp = sessionTimestamps.get(token);
        if (timestamp == null || System.currentTimeMillis() - timestamp > SESSION_TIMEOUT) {
            invalidateSession(token);
            return null;
        }
        
        return sessions.get(token);
    }

    /**
     * Invalidate a session
     * @param token Session token
     */
    public void invalidateSession(String token) {
        sessions.remove(token);
        sessionTimestamps.remove(token);
    }

    /**
     * Check if session is valid
     * @param token Session token
     * @return true if session is valid
     */
    public boolean isValidSession(String token) {
        return getUser(token) != null;
    }
    
    /**
     * Clear all sessions (logout)
     */
    public void clearSession() {
        sessions.clear();
        sessionTimestamps.clear();
    }
    
    /**
     * Get current user from any active session
     * @return Current user if any session exists, null otherwise
     */
    public User getCurrentUser() {
        if (sessions.isEmpty()) {
            return null;
        }
        // Return first valid user from sessions
        for (Map.Entry<String, User> entry : sessions.entrySet()) {
            if (isValidSession(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}



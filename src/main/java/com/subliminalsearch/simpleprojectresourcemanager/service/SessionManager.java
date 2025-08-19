package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages user sessions and auto-logout functionality
 */
public class SessionManager {
    
    private static SessionManager instance;
    
    private User currentUser;
    private String sessionToken;
    private LocalDateTime sessionStartTime;
    private LocalDateTime lastActivityTime;
    
    // Session configuration
    private static final int SESSION_TIMEOUT_MINUTES = 30;  // Auto-logout after 30 minutes of inactivity
    private static final int WARNING_BEFORE_TIMEOUT_MINUTES = 5;  // Warn 5 minutes before timeout
    
    private Timeline inactivityTimer;
    private AtomicLong lastActivityTimestamp = new AtomicLong(System.currentTimeMillis());
    private boolean warningShown = false;
    
    // Callback for when session expires
    private Runnable onSessionExpired;
    
    private SessionManager() {
        setupInactivityTimer();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Create a new session for the authenticated user
     */
    public void createSession(User user) {
        this.currentUser = user;
        this.sessionToken = UUID.randomUUID().toString();
        this.sessionStartTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
        
        user.setSessionToken(sessionToken);
        
        // Reset activity tracking
        updateActivity();
        warningShown = false;
        
        // Start inactivity timer
        if (inactivityTimer != null) {
            inactivityTimer.play();
        }
        
        System.out.println("Session created for user: " + user.getUsername() + 
                          " with role: " + user.getRole());
    }
    
    /**
     * Setup the inactivity timer
     */
    private void setupInactivityTimer() {
        inactivityTimer = new Timeline(
            new KeyFrame(Duration.minutes(1), e -> checkInactivity())
        );
        inactivityTimer.setCycleCount(Timeline.INDEFINITE);
    }
    
    /**
     * Check for user inactivity
     */
    private void checkInactivity() {
        if (currentUser == null) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long lastActivity = lastActivityTimestamp.get();
        long inactiveMinutes = (now - lastActivity) / 60000;
        
        // Check if we need to show warning
        if (!warningShown && 
            inactiveMinutes >= (SESSION_TIMEOUT_MINUTES - WARNING_BEFORE_TIMEOUT_MINUTES)) {
            
            Platform.runLater(() -> showInactivityWarning());
            warningShown = true;
        }
        
        // Check if session should expire
        if (inactiveMinutes >= SESSION_TIMEOUT_MINUTES) {
            Platform.runLater(() -> expireSession("Session expired due to inactivity"));
        }
    }
    
    /**
     * Show inactivity warning to user
     */
    private void showInactivityWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Session Timeout Warning");
        alert.setHeaderText("Your session will expire soon");
        alert.setContentText("Your session will expire in " + WARNING_BEFORE_TIMEOUT_MINUTES + 
                            " minutes due to inactivity. Click OK to continue working.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateActivity();
            warningShown = false;
        }
    }
    
    /**
     * Update last activity time
     */
    public void updateActivity() {
        lastActivityTimestamp.set(System.currentTimeMillis());
        lastActivityTime = LocalDateTime.now();
    }
    
    /**
     * End the current session
     */
    public void endSession() {
        if (currentUser != null) {
            System.out.println("Session ended for user: " + currentUser.getUsername());
        }
        
        currentUser = null;
        sessionToken = null;
        sessionStartTime = null;
        lastActivityTime = null;
        warningShown = false;
        
        if (inactivityTimer != null) {
            inactivityTimer.stop();
        }
    }
    
    /**
     * Expire session and show login screen
     */
    private void expireSession(String reason) {
        System.out.println("Session expired: " + reason);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Expired");
        alert.setHeaderText("Your session has expired");
        alert.setContentText(reason + ". Please log in again.");
        alert.showAndWait();
        
        endSession();
        
        if (onSessionExpired != null) {
            onSessionExpired.run();
        }
    }
    
    /**
     * Check if user has permission for an action
     */
    public boolean hasPermission(String permission) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.hasPermission(permission);
    }
    
    /**
     * Check if user is in a specific AD group
     */
    public boolean isInGroup(String groupName) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.isMemberOf(groupName);
    }
    
    /**
     * Check if current session is valid
     */
    public boolean isSessionValid() {
        if (currentUser == null || sessionToken == null) {
            return false;
        }
        
        // Check if session has expired
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = sessionStartTime.plusMinutes(SESSION_TIMEOUT_MINUTES);
        
        return now.isBefore(expiryTime);
    }
    
    /**
     * Get session info for display
     */
    public String getSessionInfo() {
        if (currentUser == null) {
            return "Not logged in";
        }
        
        return String.format("User: %s | Role: %s | Session: %s", 
                           currentUser.getFullName(),
                           currentUser.getRole(),
                           sessionStartTime.toLocalTime());
    }
    
    // Getters and setters
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }
    
    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }
    
    public void setOnSessionExpired(Runnable callback) {
        this.onSessionExpired = callback;
    }
    
    public LDAPService.UserRole getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
    
    public boolean isProjectManager() {
        return currentUser != null && 
               currentUser.getRole() == LDAPService.UserRole.PROJECT_MANAGER;
    }
    
    public boolean isTechnician() {
        return currentUser != null && 
               currentUser.getRole() == LDAPService.UserRole.TECHNICIAN;
    }
    
    public boolean isAdmin() {
        return currentUser != null && 
               currentUser.getRole() == LDAPService.UserRole.ADMIN;
    }
}
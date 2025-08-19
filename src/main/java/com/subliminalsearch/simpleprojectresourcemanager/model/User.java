package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.util.List;
import com.subliminalsearch.simpleprojectresourcemanager.service.LDAPService.UserRole;

/**
 * User model for LDAP/AD authenticated users
 */
public class User {
    private String username;        // AD sAMAccountName
    private String fullName;        // Display name
    private String email;
    private String department;
    private String title;           // Job title
    private String phone;
    private String manager;          // Manager's DN
    private List<String> groups;    // AD group memberships
    private UserRole role;          // Application role
    
    // Additional application-specific fields
    private Long resourceId;        // Link to Resource table
    private boolean active = true;
    private String sessionToken;     // For session management
    
    // Constructors
    public User() {}
    
    public User(String username) {
        this.username = username;
    }
    
    // Check if user has specific permission
    public boolean hasPermission(String permission) {
        switch (role) {
            case ADMIN:
                return true; // Admins have all permissions
            case PROJECT_MANAGER:
                return permission.equals("CREATE_PROJECT") || 
                       permission.equals("EDIT_PROJECT") ||
                       permission.equals("ASSIGN_RESOURCES") ||
                       permission.equals("VIEW_ALL");
            case SUPERVISOR:
                return permission.equals("VIEW_ALL") ||
                       permission.equals("EDIT_ASSIGNMENTS");
            case TECHNICIAN:
                return permission.equals("VIEW_OWN") ||
                       permission.equals("UPDATE_STATUS");
            default:
                return permission.equals("VIEW_OWN");
        }
    }
    
    // Check if user is in specific AD group
    public boolean isMemberOf(String groupName) {
        return groups != null && groups.contains(groupName);
    }
    
    // Check if user can access project
    public boolean canAccessProject(String projectId) {
        // Admins and PMs can access all projects
        if (role == UserRole.ADMIN || role == UserRole.PROJECT_MANAGER) {
            return true;
        }
        
        // Others need specific assignment or team membership
        // This would be checked against the database
        return false;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    
    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    
    @Override
    public String toString() {
        return fullName != null ? fullName : username;
    }
}
package com.subliminalsearch.simpleprojectresourcemanager.model;

public enum ProjectStatus {
    PLANNED("Planned"),
    ACTIVE("Active"),
    COMPLETED("Completed"), 
    DELAYED("Delayed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
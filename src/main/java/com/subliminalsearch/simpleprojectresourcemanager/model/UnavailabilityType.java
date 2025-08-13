package com.subliminalsearch.simpleprojectresourcemanager.model;

public enum UnavailabilityType {
    VACATION("Vacation"),
    SICK_LEAVE("Sick Leave"), 
    PERSONAL_TIME("Personal Time"),
    TRAINING("Training/Certification"),
    OTHER_ASSIGNMENT("Other Assignment"),
    RECURRING("Recurring Unavailability"),
    EMERGENCY("Emergency/Unplanned");
    
    private final String displayName;
    
    UnavailabilityType(String displayName) {
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
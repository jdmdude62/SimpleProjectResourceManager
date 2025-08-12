package com.subliminalsearch.simpleprojectresourcemanager.model;

public enum HolidayType {
    FEDERAL("Federal Holiday"),
    COMPANY("Company Holiday"),
    FLOATING("Floating Holiday"),
    DEPARTMENT("Department Specific"),
    EMERGENCY_CLOSURE("Emergency Closure"),
    HALF_DAY("Half Day");
    
    private final String displayName;
    
    HolidayType(String displayName) {
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
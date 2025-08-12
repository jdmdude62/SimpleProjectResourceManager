package com.subliminalsearch.simpleprojectresourcemanager.model;

public enum ResourceCategory {
    INTERNAL("Internal Employee"),
    CONTRACTOR("3rd Party Contractor"),
    VENDOR("Vendor");
    
    private final String displayName;
    
    ResourceCategory(String displayName) {
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
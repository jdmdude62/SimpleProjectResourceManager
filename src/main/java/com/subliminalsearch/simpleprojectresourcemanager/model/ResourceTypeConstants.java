package com.subliminalsearch.simpleprojectresourcemanager.model;

/**
 * Constants for common resource types
 */
public class ResourceTypeConstants {
    
    // Common resource types
    public static final ResourceType EMPLOYEE = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
    public static final ResourceType PART_TIME_EMPLOYEE = new ResourceType("Part-Time Employee", ResourceCategory.INTERNAL);
    public static final ResourceType TECHNICIAN = new ResourceType("Field Technician", ResourceCategory.INTERNAL);
    public static final ResourceType CONTRACTOR = new ResourceType("Contractor", ResourceCategory.CONTRACTOR);
    public static final ResourceType VENDOR = new ResourceType("Vendor", ResourceCategory.VENDOR);
    
    // Check if a resource type is for field technicians
    public static boolean isFieldTechnician(ResourceType type) {
        if (type == null) return false;
        return type.getName() != null && 
               (type.getName().toLowerCase().contains("technician") || 
                type.getName().toLowerCase().contains("field"));
    }
    
    // Check if a resource type is internal employee
    public static boolean isInternalEmployee(ResourceType type) {
        if (type == null) return false;
        return type.getCategory() == ResourceCategory.INTERNAL;
    }
    
    // Get resource type for AD users
    public static ResourceType getTypeForADGroup(String groupName) {
        if (groupName == null) return EMPLOYEE;
        
        if (groupName.equalsIgnoreCase("CyberMetal") || 
            groupName.toLowerCase().contains("technician")) {
            return TECHNICIAN;
        }
        
        if (groupName.toLowerCase().contains("contractor")) {
            return CONTRACTOR;
        }
        
        if (groupName.toLowerCase().contains("vendor")) {
            return VENDOR;
        }
        
        return EMPLOYEE;
    }
}
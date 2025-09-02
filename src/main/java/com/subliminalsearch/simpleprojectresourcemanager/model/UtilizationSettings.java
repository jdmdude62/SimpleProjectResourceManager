package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.io.Serializable;

/**
 * Configuration settings for resource utilization calculations
 */
public class UtilizationSettings implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Basic Settings
    private boolean includeWeekends = false;
    private boolean includeSaturdays = false; // For partial weekend work
    private boolean includeHolidays = false;
    private boolean countPtoAsUtilized = false;
    private boolean countShopAsUtilized = true;
    private boolean countTrainingAsUtilized = true;
    
    // Working Hours Definition
    private double standardWorkWeek = 40.0; // hours per week
    private double hoursPerDay = 8.0; // hours per day
    private double overtimeThreshold = 100.0; // percentage
    
    // Utilization Targets (for color coding)
    private double targetUtilization = 80.0; // percentage
    private double minimumUtilization = 65.0; // percentage
    private double overallocationAlert = 110.0; // percentage
    
    // Billable Targets
    private double targetBillable = 75.0; // percentage
    private double minimumBillable = 60.0; // percentage
    
    // Calculation Method
    public enum CalculationMethod {
        CALENDAR_DAYS("Calendar Days", "All days in period"),
        WORKING_DAYS("Working Days", "Exclude weekends/holidays"),
        AVAILABLE_HOURS("Available Hours", "Based on work schedule"),
        CUSTOM_FORMULA("Custom Formula", "Advanced configuration");
        
        private final String displayName;
        private final String description;
        
        CalculationMethod(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    private CalculationMethod calculationMethod = CalculationMethod.WORKING_DAYS;
    
    // Industry Presets
    public enum IndustryPreset {
        FIELD_SERVICE("Field Service / Construction", 
            "Techs work on-site including some Saturdays"),
        PROFESSIONAL_SERVICES("Professional Services / Consulting", 
            "Billable hours are everything"),
        MANUFACTURING("Manufacturing / Shop-Based", 
            "Mix of shop work and field installations"),
        EMERGENCY_SERVICES("Emergency Services / 24-7 Operations", 
            "Round-the-clock coverage with rotating shifts"),
        GOVERNMENT_CONTRACTOR("Government Contractor", 
            "Strict hour tracking with contract requirements"),
        MAINTENANCE_REPAIR("Maintenance & Repair", 
            "Scheduled maintenance with emergency response"),
        CUSTOM("Custom / Start Fresh", 
            "Define your own parameters");
        
        private final String displayName;
        private final String description;
        
        IndustryPreset(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Default constructor
    public UtilizationSettings() {
        // Defaults are already set in field declarations
    }
    
    // Apply industry preset
    public void applyPreset(IndustryPreset preset) {
        switch (preset) {
            case FIELD_SERVICE:
                includeWeekends = false;
                includeSaturdays = true;
                includeHolidays = false;
                countPtoAsUtilized = false;
                countShopAsUtilized = true;
                standardWorkWeek = 45.0;
                hoursPerDay = 9.0;
                targetUtilization = 85.0;
                minimumUtilization = 70.0;
                targetBillable = 70.0;
                minimumBillable = 55.0;
                calculationMethod = CalculationMethod.WORKING_DAYS;
                break;
                
            case PROFESSIONAL_SERVICES:
                includeWeekends = false;
                includeSaturdays = false;
                includeHolidays = false;
                countPtoAsUtilized = false;
                countShopAsUtilized = false;
                standardWorkWeek = 40.0;
                hoursPerDay = 8.0;
                targetUtilization = 90.0;
                minimumUtilization = 75.0;
                targetBillable = 85.0;
                minimumBillable = 70.0;
                calculationMethod = CalculationMethod.WORKING_DAYS;
                break;
                
            case MANUFACTURING:
                includeWeekends = false;
                includeSaturdays = false;
                includeHolidays = false;
                countPtoAsUtilized = false;
                countShopAsUtilized = true;
                standardWorkWeek = 40.0;
                hoursPerDay = 8.0;
                targetUtilization = 95.0;
                minimumUtilization = 80.0;
                targetBillable = 60.0;
                minimumBillable = 45.0;
                calculationMethod = CalculationMethod.WORKING_DAYS;
                break;
                
            case EMERGENCY_SERVICES:
                includeWeekends = true;
                includeSaturdays = true;
                includeHolidays = true;
                countPtoAsUtilized = true;
                countShopAsUtilized = true;
                standardWorkWeek = 40.0;
                hoursPerDay = 8.0;
                targetUtilization = 75.0;
                minimumUtilization = 60.0;
                targetBillable = 65.0;
                minimumBillable = 50.0;
                calculationMethod = CalculationMethod.CALENDAR_DAYS;
                break;
                
            case GOVERNMENT_CONTRACTOR:
                includeWeekends = false;
                includeSaturdays = false;
                includeHolidays = false;
                countPtoAsUtilized = false;
                countShopAsUtilized = true;
                standardWorkWeek = 40.0;
                hoursPerDay = 8.0;
                targetUtilization = 95.0;
                minimumUtilization = 85.0;
                targetBillable = 80.0;
                minimumBillable = 65.0;
                calculationMethod = CalculationMethod.WORKING_DAYS;
                break;
                
            case MAINTENANCE_REPAIR:
                includeWeekends = false;
                includeSaturdays = false;
                includeHolidays = false;
                countPtoAsUtilized = false;
                countShopAsUtilized = true;
                standardWorkWeek = 40.0;
                hoursPerDay = 8.0;
                targetUtilization = 75.0;
                minimumUtilization = 60.0;
                targetBillable = 65.0;
                minimumBillable = 50.0;
                calculationMethod = CalculationMethod.WORKING_DAYS;
                break;
                
            case CUSTOM:
            default:
                // Keep current settings or reset to defaults
                break;
        }
    }
    
    // Getters and Setters
    public boolean isIncludeWeekends() { return includeWeekends; }
    public void setIncludeWeekends(boolean includeWeekends) { this.includeWeekends = includeWeekends; }
    
    public boolean isIncludeSaturdays() { return includeSaturdays; }
    public void setIncludeSaturdays(boolean includeSaturdays) { this.includeSaturdays = includeSaturdays; }
    
    public boolean isIncludeHolidays() { return includeHolidays; }
    public void setIncludeHolidays(boolean includeHolidays) { this.includeHolidays = includeHolidays; }
    
    public boolean isCountPtoAsUtilized() { return countPtoAsUtilized; }
    public void setCountPtoAsUtilized(boolean countPtoAsUtilized) { this.countPtoAsUtilized = countPtoAsUtilized; }
    
    public boolean isCountShopAsUtilized() { return countShopAsUtilized; }
    public void setCountShopAsUtilized(boolean countShopAsUtilized) { this.countShopAsUtilized = countShopAsUtilized; }
    
    public boolean isCountTrainingAsUtilized() { return countTrainingAsUtilized; }
    public void setCountTrainingAsUtilized(boolean countTrainingAsUtilized) { this.countTrainingAsUtilized = countTrainingAsUtilized; }
    
    public double getStandardWorkWeek() { return standardWorkWeek; }
    public void setStandardWorkWeek(double standardWorkWeek) { this.standardWorkWeek = standardWorkWeek; }
    
    public double getHoursPerDay() { return hoursPerDay; }
    public void setHoursPerDay(double hoursPerDay) { this.hoursPerDay = hoursPerDay; }
    
    public double getOvertimeThreshold() { return overtimeThreshold; }
    public void setOvertimeThreshold(double overtimeThreshold) { this.overtimeThreshold = overtimeThreshold; }
    
    public double getTargetUtilization() { return targetUtilization; }
    public void setTargetUtilization(double targetUtilization) { this.targetUtilization = targetUtilization; }
    
    public double getMinimumUtilization() { return minimumUtilization; }
    public void setMinimumUtilization(double minimumUtilization) { this.minimumUtilization = minimumUtilization; }
    
    public double getOverallocationAlert() { return overallocationAlert; }
    public void setOverallocationAlert(double overallocationAlert) { this.overallocationAlert = overallocationAlert; }
    
    public double getTargetBillable() { return targetBillable; }
    public void setTargetBillable(double targetBillable) { this.targetBillable = targetBillable; }
    
    public double getMinimumBillable() { return minimumBillable; }
    public void setMinimumBillable(double minimumBillable) { this.minimumBillable = minimumBillable; }
    
    public CalculationMethod getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(CalculationMethod calculationMethod) { this.calculationMethod = calculationMethod; }
}
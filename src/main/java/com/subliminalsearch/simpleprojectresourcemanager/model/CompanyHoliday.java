package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class CompanyHoliday {
    private Long id;
    private String name;
    private LocalDate date;
    private HolidayType type;
    private String description;
    private boolean isRecurring;
    private String recurrenceRule; // e.g., "ANNUAL:THIRD_MONDAY_JANUARY" 
    private boolean workingHolidayAllowed; // Can people work through this holiday?
    private String department; // null means company-wide
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public CompanyHoliday() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        this.workingHolidayAllowed = false;
        this.isRecurring = false;
    }
    
    public CompanyHoliday(String name, LocalDate date, HolidayType type) {
        this();
        this.name = name;
        this.date = date;
        this.type = type;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
        this.updatedAt = LocalDateTime.now();
    }
    
    public HolidayType getType() {
        return type;
    }
    
    public void setType(HolidayType type) {
        this.type = type;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getRecurrenceRule() {
        return recurrenceRule;
    }
    
    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isWorkingHolidayAllowed() {
        return workingHolidayAllowed;
    }
    
    public void setWorkingHolidayAllowed(boolean workingHolidayAllowed) {
        this.workingHolidayAllowed = workingHolidayAllowed;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Utility methods
    public boolean isActiveOn(LocalDate checkDate) {
        return active && date != null && date.equals(checkDate);
    }
    
    // Convenience methods for UI compatibility
    public boolean isWorkingDay() {
        return workingHolidayAllowed;
    }
    
    public void setWorkingDay(boolean workingDay) {
        this.workingHolidayAllowed = workingDay;
    }
    
    public boolean appliesToDepartment(String resourceDepartment) {
        // If holiday department is null, applies to all departments
        // If resource department is null, applies to all company holidays
        return department == null || Objects.equals(department, resourceDepartment);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanyHoliday that = (CompanyHoliday) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("CompanyHoliday{id=%d, name='%s', date=%s, type=%s, department='%s', active=%s}", 
                id, name, date, type, department, active);
    }
}
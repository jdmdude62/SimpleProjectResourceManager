package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class TechnicianUnavailability {
    private Long id;
    private Long resourceId;
    private UnavailabilityType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String description;
    private boolean approved;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private boolean isRecurring;
    private String recurrencePattern; // e.g., "WEEKLY:FRIDAY" or "MONTHLY:LAST_FRIDAY"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public TechnicianUnavailability() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.approved = false;
        this.isRecurring = false;
    }
    
    public TechnicianUnavailability(Long resourceId, UnavailabilityType type, LocalDate startDate, LocalDate endDate) {
        this();
        this.resourceId = resourceId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public UnavailabilityType getType() {
        return type;
    }
    
    public void setType(UnavailabilityType type) {
        this.type = type;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isApproved() {
        return approved;
    }
    
    public void setApproved(boolean approved) {
        this.approved = approved;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public boolean isRecurring() {
        return isRecurring;
    }
    
    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getRecurrencePattern() {
        return recurrencePattern;
    }
    
    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
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
    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) startDate.until(endDate).getDays() + 1;
    }
    
    public boolean isActiveOn(LocalDate date) {
        if (startDate == null || endDate == null || date == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        if (startDate == null || endDate == null || otherStart == null || otherEnd == null) {
            return false;
        }
        return !endDate.isBefore(otherStart) && !otherEnd.isBefore(startDate);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicianUnavailability that = (TechnicianUnavailability) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("TechnicianUnavailability{id=%d, resourceId=%d, type=%s, dates=%s to %s, approved=%s}", 
                id, resourceId, type, startDate, endDate, approved);
    }
}
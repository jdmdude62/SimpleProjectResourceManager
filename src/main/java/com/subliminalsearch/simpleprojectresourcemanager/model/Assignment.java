package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Assignment {
    private Long id;
    private Long projectId;
    private Long resourceId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int travelOutDays;
    private int travelBackDays;
    private boolean isOverride;
    private String overrideReason;
    private String notes;
    private String location;  // Location/phase for multi-location projects
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields for display purposes
    private Project project;
    private Resource resource;

    public Assignment() {
        this.travelOutDays = 0;
        this.travelBackDays = 0;
        this.isOverride = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Assignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate) {
        this();
        this.projectId = projectId;
        this.resourceId = resourceId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Assignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate, 
                     int travelOutDays, int travelBackDays) {
        this(projectId, resourceId, startDate, endDate);
        this.travelOutDays = travelOutDays;
        this.travelBackDays = travelBackDays;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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

    public int getTravelOutDays() {
        return travelOutDays;
    }

    public void setTravelOutDays(int travelOutDays) {
        this.travelOutDays = travelOutDays;
        this.updatedAt = LocalDateTime.now();
    }

    public int getTravelBackDays() {
        return travelBackDays;
    }

    public void setTravelBackDays(int travelBackDays) {
        this.travelBackDays = travelBackDays;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverride() {
        return isOverride;
    }

    public void setOverride(boolean override) {
        isOverride = override;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
        this.updatedAt = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public LocalDate getEffectiveStartDate() {
        if (startDate == null) return null;
        return startDate.minusDays(travelOutDays);
    }

    public LocalDate getEffectiveEndDate() {
        if (endDate == null) return null;
        return endDate.plusDays(travelBackDays);
    }

    public int getTotalDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return (int) startDate.until(endDate).getDays() + 1 + travelOutDays + travelBackDays;
    }

    public int getWorkDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return (int) startDate.until(endDate).getDays() + 1;
    }

    public boolean overlapsWithDates(LocalDate otherStart, LocalDate otherEnd) {
        if (getEffectiveStartDate() == null || getEffectiveEndDate() == null || 
            otherStart == null || otherEnd == null) {
            return false;
        }
        
        return !(getEffectiveEndDate().isBefore(otherStart) || 
                getEffectiveStartDate().isAfter(otherEnd));
    }

    public boolean overlapsWith(Assignment other) {
        return overlapsWithDates(other.getEffectiveStartDate(), other.getEffectiveEndDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Assignment{project=%s, resource=%s, dates=%s to %s, travel=%d/%d}", 
                projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
    }
}
package com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.models;

import java.time.LocalDate;

public class SPAssignment {
    private Integer id;
    private String title;  // Auto-generated: Resource - Project
    private String assignmentId;
    private String resourceId;
    private String projectId;
    private Integer resourceLookupId;  // SharePoint lookup field ID
    private Integer projectLookupId;   // SharePoint lookup field ID
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String notes;
    private Integer travelDays;
    private String etag;
    
    public SPAssignment() {
    }
    
    public SPAssignment(String assignmentId, String resourceId, String projectId) {
        this.assignmentId = assignmentId;
        this.resourceId = resourceId;
        this.projectId = projectId;
    }
    
    // Getters and setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public Integer getResourceLookupId() {
        return resourceLookupId;
    }
    
    public void setResourceLookupId(Integer resourceLookupId) {
        this.resourceLookupId = resourceLookupId;
    }
    
    public Integer getProjectLookupId() {
        return projectLookupId;
    }
    
    public void setProjectLookupId(Integer projectLookupId) {
        this.projectLookupId = projectLookupId;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Integer getTravelDays() {
        return travelDays;
    }
    
    public void setTravelDays(Integer travelDays) {
        this.travelDays = travelDays;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
}
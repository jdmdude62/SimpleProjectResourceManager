package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ChangeOrder {
    private Long id;
    private Long projectId;
    private String changeOrderNumber;
    private String description;
    private Double additionalCost;
    private ChangeReason reason;
    private ChangeStatus status;
    private LocalDate requestDate;
    private LocalDate approvalDate;
    private String requestedBy;
    private String approvedBy;
    private Integer additionalDays; // Extension to project timeline
    private String impactDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum ChangeReason {
        CLIENT_REQUEST("Client Request"),
        DESIGN_ERROR("Design Error"),
        SITE_CONDITIONS("Unforeseen Site Conditions"),
        WEATHER_DELAY("Weather Delay"),
        MATERIAL_UNAVAILABLE("Material Unavailable"),
        SCOPE_ADDITION("Scope Addition"),
        REGULATORY_CHANGE("Regulatory Change"),
        ERROR_CORRECTION("Error Correction"),
        OTHER("Other");
        
        private final String displayName;
        
        ChangeReason(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public enum ChangeStatus {
        DRAFT("Draft"),
        SUBMITTED("Submitted"),
        UNDER_REVIEW("Under Review"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed");
        
        private final String displayName;
        
        ChangeStatus(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public ChangeOrder() {
        this.status = ChangeStatus.DRAFT;
        this.requestDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public String getChangeOrderNumber() { return changeOrderNumber; }
    public void setChangeOrderNumber(String changeOrderNumber) { this.changeOrderNumber = changeOrderNumber; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getAdditionalCost() { return additionalCost; }
    public void setAdditionalCost(Double additionalCost) { this.additionalCost = additionalCost; }
    
    public ChangeReason getReason() { return reason; }
    public void setReason(ChangeReason reason) { this.reason = reason; }
    
    public ChangeStatus getStatus() { return status; }
    public void setStatus(ChangeStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == ChangeStatus.APPROVED && approvalDate == null) {
            this.approvalDate = LocalDate.now();
        }
    }
    
    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }
    
    public LocalDate getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }
    
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public Integer getAdditionalDays() { return additionalDays; }
    public void setAdditionalDays(Integer additionalDays) { this.additionalDays = additionalDays; }
    
    public String getImpactDescription() { return impactDescription; }
    public void setImpactDescription(String impactDescription) { this.impactDescription = impactDescription; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
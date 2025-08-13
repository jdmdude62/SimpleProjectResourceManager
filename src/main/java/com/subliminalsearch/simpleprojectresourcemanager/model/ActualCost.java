package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ActualCost {
    private Long id;
    private Long projectId;
    private LocalDate costDate;
    private CostCategory category;
    private String description;
    private Double amount;
    private String invoiceNumber;
    private String receiptNumber;
    private CostStatus status;
    private Double estimatedAmount; // Original estimate for variance calculation
    private String notes;
    private Long purchaseOrderId; // Link to PO if applicable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum CostCategory {
        LABOR("Labor"),
        MATERIALS("Materials"),
        TRAVEL("Travel"),
        EQUIPMENT("Equipment"),
        SUBCONTRACTOR("Subcontractor"),
        PERMITS("Permits/Licenses"),
        OTHER("Other");
        
        private final String displayName;
        
        CostCategory(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public enum CostStatus {
        PENDING("Pending Verification"),
        VERIFIED("Verified"),
        APPROVED("Approved"),
        PAID("Paid"),
        DISPUTED("Disputed"),
        REJECTED("Rejected");
        
        private final String displayName;
        
        CostStatus(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public ActualCost() {
        this.costDate = LocalDate.now();
        this.status = CostStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Calculate variance from estimate
    public Double getVariance() {
        if (estimatedAmount != null && amount != null) {
            return amount - estimatedAmount;
        }
        return null;
    }
    
    public Double getVariancePercentage() {
        if (estimatedAmount != null && estimatedAmount > 0 && amount != null) {
            return ((amount - estimatedAmount) / estimatedAmount) * 100;
        }
        return null;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public LocalDate getCostDate() { return costDate; }
    public void setCostDate(LocalDate costDate) { this.costDate = costDate; }
    
    public CostCategory getCategory() { return category; }
    public void setCategory(CostCategory category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    
    public CostStatus getStatus() { return status; }
    public void setStatus(CostStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Double getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(Double estimatedAmount) { this.estimatedAmount = estimatedAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
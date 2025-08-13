package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseOrder {
    private Long id;
    private Long projectId;
    private String poNumber;
    private String vendor;
    private String description;
    private Double amount;
    private POStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;
    private String invoiceNumber;
    private Long resourceId; // Optional link to resource
    private Long assignmentId; // Optional link to assignment
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum POStatus {
        DRAFT("Draft"),
        PENDING("Pending Approval"),
        APPROVED("Approved"),
        ORDERED("Ordered"),
        RECEIVED("Received"),
        PAID("Paid"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        POStatus(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public PurchaseOrder() {
        this.status = POStatus.DRAFT;
        this.orderDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public POStatus getStatus() { return status; }
    public void setStatus(POStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { 
        this.receivedDate = receivedDate;
        if (receivedDate != null && status != POStatus.PAID) {
            this.status = POStatus.RECEIVED;
        }
    }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
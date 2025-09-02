package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class OpenItem {
    private Long id;
    private Long projectId;
    private Long taskId;
    
    // Basic Information
    private String itemNumber;
    private String title;
    private String description;
    private String category;
    private Priority priority = Priority.MEDIUM;
    
    // Progress Tracking
    private LocalDate estimatedStartDate;
    private LocalDate estimatedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private int progressPercentage = 0;
    
    // Status Management
    private ItemStatus status = ItemStatus.NOT_STARTED;
    private HealthStatus healthStatus = HealthStatus.ON_TRACK;
    
    // Assignment
    private String assignedTo;
    private Long assignedResourceId;
    
    // Dependencies
    private Long dependsOnItemId;
    private String blocksItemIds;
    
    // Additional Fields
    private String notes;
    private String tags;
    private Double estimatedHours;
    private Double actualHours;
    
    // Metadata
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    
    // Soft delete
    private boolean isDeleted = false;
    private LocalDateTime deletedAt;
    
    // Enums for status fields
    public enum Priority {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low");
        
        private final String displayName;
        
        Priority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ItemStatus {
        NOT_STARTED("Not Started"),
        IN_PROGRESS("In Progress"),
        ON_HOLD("On Hold"),
        BLOCKED("Blocked"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        ItemStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum HealthStatus {
        ON_TRACK("On Track"),
        AT_RISK("At Risk"),
        DELAYED("Delayed"),
        CRITICAL("Critical");
        
        private final String displayName;
        
        HealthStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColorCode() {
            switch (this) {
                case ON_TRACK: return "#28a745"; // Green
                case AT_RISK: return "#ffc107";  // Yellow
                case DELAYED: return "#fd7e14";  // Orange
                case CRITICAL: return "#dc3545"; // Red
                default: return "#6c757d";       // Gray
            }
        }
    }
    
    // Constructors
    public OpenItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Set default values
        this.priority = Priority.MEDIUM;
        this.status = ItemStatus.NOT_STARTED;
        this.healthStatus = HealthStatus.ON_TRACK;
        this.progressPercentage = 0;
        this.isDeleted = false;
    }
    
    public OpenItem(Long projectId, String title) {
        this();
        this.projectId = projectId;
        this.title = title;
    }
    
    // Utility methods
    public boolean isOverdue() {
        if (status == ItemStatus.COMPLETED || status == ItemStatus.CANCELLED) {
            return false;
        }
        if (estimatedEndDate != null) {
            return LocalDate.now().isAfter(estimatedEndDate);
        }
        return false;
    }
    
    public int getDaysRemaining() {
        if (estimatedEndDate != null && !isCompleted()) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), estimatedEndDate);
        }
        return 0;
    }
    
    public boolean isCompleted() {
        return status == ItemStatus.COMPLETED;
    }
    
    public boolean isActive() {
        return !isDeleted && status != ItemStatus.CANCELLED;
    }
    
    public void updateProgress(int percentage) {
        this.progressPercentage = Math.min(100, Math.max(0, percentage));
        this.updatedAt = LocalDateTime.now();
        
        // Auto-update status based on progress
        if (progressPercentage == 0 && status == ItemStatus.IN_PROGRESS) {
            status = ItemStatus.NOT_STARTED;
        } else if (progressPercentage > 0 && progressPercentage < 100 && status == ItemStatus.NOT_STARTED) {
            status = ItemStatus.IN_PROGRESS;
        } else if (progressPercentage == 100 && status != ItemStatus.COMPLETED) {
            status = ItemStatus.COMPLETED;
            actualEndDate = LocalDate.now();
        }
    }
    
    // Getters and Setters
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
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getItemNumber() {
        return itemNumber;
    }
    
    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDate getEstimatedStartDate() {
        return estimatedStartDate;
    }
    
    public void setEstimatedStartDate(LocalDate estimatedStartDate) {
        this.estimatedStartDate = estimatedStartDate;
    }
    
    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }
    
    public void setEstimatedEndDate(LocalDate estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }
    
    public LocalDate getActualStartDate() {
        return actualStartDate;
    }
    
    public void setActualStartDate(LocalDate actualStartDate) {
        this.actualStartDate = actualStartDate;
    }
    
    public LocalDate getActualEndDate() {
        return actualEndDate;
    }
    
    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }
    
    public int getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(int progressPercentage) {
        updateProgress(progressPercentage);
    }
    
    public ItemStatus getStatus() {
        return status;
    }
    
    public void setStatus(ItemStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        
        // Auto-set actual dates
        if (status == ItemStatus.IN_PROGRESS && actualStartDate == null) {
            actualStartDate = LocalDate.now();
        } else if (status == ItemStatus.COMPLETED && actualEndDate == null) {
            actualEndDate = LocalDate.now();
        }
    }
    
    public HealthStatus getHealthStatus() {
        return healthStatus;
    }
    
    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public Long getAssignedResourceId() {
        return assignedResourceId;
    }
    
    public void setAssignedResourceId(Long assignedResourceId) {
        this.assignedResourceId = assignedResourceId;
    }
    
    public Long getDependsOnItemId() {
        return dependsOnItemId;
    }
    
    public void setDependsOnItemId(Long dependsOnItemId) {
        this.dependsOnItemId = dependsOnItemId;
    }
    
    public String getBlocksItemIds() {
        return blocksItemIds;
    }
    
    public void setBlocksItemIds(String blocksItemIds) {
        this.blocksItemIds = blocksItemIds;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Double getEstimatedHours() {
        return estimatedHours;
    }
    
    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
    
    public Double getActualHours() {
        return actualHours;
    }
    
    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
        if (deleted && deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenItem openItem = (OpenItem) o;
        return Objects.equals(id, openItem.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "OpenItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", progress=" + progressPercentage + "%" +
                ", health=" + healthStatus +
                '}';
    }
}
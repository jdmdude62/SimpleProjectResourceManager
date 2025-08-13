package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task {
    private Long id;
    private Long projectId;
    private Long phaseId;
    private Long parentTaskId;
    private String taskCode;
    private String title;
    private String description;
    
    // Task categorization
    private TaskType taskType = TaskType.GENERAL;
    private TaskPriority priority = TaskPriority.MEDIUM;
    private TaskStatus status = TaskStatus.NOT_STARTED;
    private Integer progressPercentage = 0;
    
    // Scheduling
    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    private Double estimatedHours;
    private Double actualHours;
    
    // Assignment
    private Long assignedTo;
    private Long assignedBy;
    private Long reviewerId;
    
    // Field service specific
    private String location;
    private String equipmentRequired;
    private String safetyRequirements;
    private String siteAccessNotes;
    
    // Risk management
    private RiskLevel riskLevel = RiskLevel.LOW;
    private String riskNotes;
    
    // Microsoft 365 integration
    private String ms365TaskId;
    private String ms365SyncStatus;
    private LocalDateTime ms365LastSync;
    
    // Tracking
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Long completedBy;
    
    // Transient fields for UI
    private String assignedToName;
    private String projectName;
    private String phaseName;
    private Integer subtaskCount;
    private Integer completedSubtaskCount;
    private Boolean hasBlockers;
    private Boolean isOnCriticalPath;
    
    public enum TaskType {
        GENERAL("General"),
        INSTALLATION("Installation"),
        CONFIGURATION("Configuration"),
        TESTING("Testing"),
        DOCUMENTATION("Documentation"),
        TRAINING("Training"),
        ASSESSMENT("Assessment"),
        REVIEW("Review");
        
        private final String displayName;
        
        TaskType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum TaskPriority {
        CRITICAL("Critical", "#FF0000"),
        HIGH("High", "#FFA500"),
        MEDIUM("Medium", "#FFFF00"),
        LOW("Low", "#00FF00");
        
        private final String displayName;
        private final String color;
        
        TaskPriority(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    public enum TaskStatus {
        NOT_STARTED("Not Started", "#808080"),
        IN_PROGRESS("In Progress", "#0080FF"),
        BLOCKED("Blocked", "#FF0000"),
        REVIEW("In Review", "#FFA500"),
        COMPLETED("Completed", "#00FF00"),
        CANCELLED("Cancelled", "#404040");
        
        private final String displayName;
        private final String color;
        
        TaskStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    public enum RiskLevel {
        HIGH("High", "#FF0000"),
        MEDIUM("Medium", "#FFA500"),
        LOW("Low", "#00FF00");
        
        private final String displayName;
        private final String color;
        
        RiskLevel(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    // Constructors
    public Task() {}
    
    public Task(String title, Long projectId) {
        this.title = title;
        this.projectId = projectId;
    }
    
    // Helper methods
    public boolean isOverdue() {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            return false;
        }
        if (plannedEnd != null) {
            return LocalDate.now().isAfter(plannedEnd);
        }
        return false;
    }
    
    public boolean isBlocked() {
        return status == TaskStatus.BLOCKED;
    }
    
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }
    
    public int getDaysRemaining() {
        if (plannedEnd != null && !isCompleted()) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), plannedEnd);
        }
        return 0;
    }
    
    public double getCompletionRate() {
        if (progressPercentage != null) {
            return progressPercentage / 100.0;
        }
        return 0.0;
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
    
    public Long getPhaseId() {
        return phaseId;
    }
    
    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }
    
    public Long getParentTaskId() {
        return parentTaskId;
    }
    
    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }
    
    public String getTaskCode() {
        return taskCode;
    }
    
    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Integer getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public LocalDate getPlannedStart() {
        return plannedStart;
    }
    
    public void setPlannedStart(LocalDate plannedStart) {
        this.plannedStart = plannedStart;
    }
    
    public LocalDate getPlannedEnd() {
        return plannedEnd;
    }
    
    public void setPlannedEnd(LocalDate plannedEnd) {
        this.plannedEnd = plannedEnd;
    }
    
    public LocalDate getActualStart() {
        return actualStart;
    }
    
    public void setActualStart(LocalDate actualStart) {
        this.actualStart = actualStart;
    }
    
    public LocalDate getActualEnd() {
        return actualEnd;
    }
    
    public void setActualEnd(LocalDate actualEnd) {
        this.actualEnd = actualEnd;
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
    
    public Long getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public Long getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public Long getReviewerId() {
        return reviewerId;
    }
    
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getEquipmentRequired() {
        return equipmentRequired;
    }
    
    public void setEquipmentRequired(String equipmentRequired) {
        this.equipmentRequired = equipmentRequired;
    }
    
    public String getSafetyRequirements() {
        return safetyRequirements;
    }
    
    public void setSafetyRequirements(String safetyRequirements) {
        this.safetyRequirements = safetyRequirements;
    }
    
    public String getSiteAccessNotes() {
        return siteAccessNotes;
    }
    
    public void setSiteAccessNotes(String siteAccessNotes) {
        this.siteAccessNotes = siteAccessNotes;
    }
    
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public String getRiskNotes() {
        return riskNotes;
    }
    
    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }
    
    public String getMs365TaskId() {
        return ms365TaskId;
    }
    
    public void setMs365TaskId(String ms365TaskId) {
        this.ms365TaskId = ms365TaskId;
    }
    
    public String getMs365SyncStatus() {
        return ms365SyncStatus;
    }
    
    public void setMs365SyncStatus(String ms365SyncStatus) {
        this.ms365SyncStatus = ms365SyncStatus;
    }
    
    public LocalDateTime getMs365LastSync() {
        return ms365LastSync;
    }
    
    public void setMs365LastSync(LocalDateTime ms365LastSync) {
        this.ms365LastSync = ms365LastSync;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getCompletedBy() {
        return completedBy;
    }
    
    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
    }
    
    public String getAssignedToName() {
        return assignedToName;
    }
    
    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getPhaseName() {
        return phaseName;
    }
    
    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }
    
    public Integer getSubtaskCount() {
        return subtaskCount;
    }
    
    public void setSubtaskCount(Integer subtaskCount) {
        this.subtaskCount = subtaskCount;
    }
    
    public Integer getCompletedSubtaskCount() {
        return completedSubtaskCount;
    }
    
    public void setCompletedSubtaskCount(Integer completedSubtaskCount) {
        this.completedSubtaskCount = completedSubtaskCount;
    }
    
    public Boolean getHasBlockers() {
        return hasBlockers;
    }
    
    public void setHasBlockers(Boolean hasBlockers) {
        this.hasBlockers = hasBlockers;
    }
    
    public Boolean getIsOnCriticalPath() {
        return isOnCriticalPath;
    }
    
    public void setIsOnCriticalPath(Boolean isOnCriticalPath) {
        this.isOnCriticalPath = isOnCriticalPath;
    }
}
package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDateTime;

public class TaskDependency {
    private Long id;
    private Long predecessorId;
    private Long successorId;
    private DependencyType dependencyType = DependencyType.FINISH_TO_START;
    private Integer lagDays = 0;
    private LocalDateTime createdAt;
    
    // Transient fields for UI
    private String predecessorTitle;
    private String successorTitle;
    
    public enum DependencyType {
        FINISH_TO_START("FS", "Finish to Start"),
        START_TO_START("SS", "Start to Start"),
        FINISH_TO_FINISH("FF", "Finish to Finish"),
        START_TO_FINISH("SF", "Start to Finish");
        
        private final String code;
        private final String displayName;
        
        DependencyType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static DependencyType fromCode(String code) {
            for (DependencyType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return FINISH_TO_START;
        }
    }
    
    // Constructors
    public TaskDependency() {}
    
    public TaskDependency(Long predecessorId, Long successorId) {
        this.predecessorId = predecessorId;
        this.successorId = successorId;
    }
    
    public TaskDependency(Long predecessorId, Long successorId, DependencyType type, Integer lagDays) {
        this.predecessorId = predecessorId;
        this.successorId = successorId;
        this.dependencyType = type;
        this.lagDays = lagDays;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPredecessorId() {
        return predecessorId;
    }
    
    public void setPredecessorId(Long predecessorId) {
        this.predecessorId = predecessorId;
    }
    
    public Long getSuccessorId() {
        return successorId;
    }
    
    public void setSuccessorId(Long successorId) {
        this.successorId = successorId;
    }
    
    public DependencyType getDependencyType() {
        return dependencyType;
    }
    
    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }
    
    public Integer getLagDays() {
        return lagDays;
    }
    
    public void setLagDays(Integer lagDays) {
        this.lagDays = lagDays;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getPredecessorTitle() {
        return predecessorTitle;
    }
    
    public void setPredecessorTitle(String predecessorTitle) {
        this.predecessorTitle = predecessorTitle;
    }
    
    public String getSuccessorTitle() {
        return successorTitle;
    }
    
    public void setSuccessorTitle(String successorTitle) {
        this.successorTitle = successorTitle;
    }
}
package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDateTime;

public class ResourceSkill {
    private Long id;
    private Long resourceId;
    private Long skillId;
    private Integer proficiencyLevel; // 1-5 scoring
    private Integer yearsOfExperience;
    private String notes;
    private LocalDateTime lastUsedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient fields for display
    private String resourceName;
    private String skillName;
    private String skillCategory;

    public ResourceSkill() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ResourceSkill(Long resourceId, Long skillId, Integer proficiencyLevel) {
        this();
        this.resourceId = resourceId;
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
    }

    // Getters and Setters
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

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(Integer proficiencyLevel) {
        if (proficiencyLevel != null && (proficiencyLevel < 1 || proficiencyLevel > 5)) {
            throw new IllegalArgumentException("Proficiency level must be between 1 and 5");
        }
        this.proficiencyLevel = proficiencyLevel;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
        this.updatedAt = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillCategory() {
        return skillCategory;
    }

    public void setSkillCategory(String skillCategory) {
        this.skillCategory = skillCategory;
    }

    public String getProficiencyDescription() {
        if (proficiencyLevel == null) return "Not Rated";
        switch (proficiencyLevel) {
            case 1: return "Beginner";
            case 2: return "Basic";
            case 3: return "Intermediate";
            case 4: return "Advanced";
            case 5: return "Expert";
            default: return "Unknown";
        }
    }
}
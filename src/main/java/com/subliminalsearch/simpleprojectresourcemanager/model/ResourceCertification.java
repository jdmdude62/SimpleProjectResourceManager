package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ResourceCertification {
    private Long id;
    private Long resourceId;
    private Long certificationId;
    private LocalDate dateObtained;
    private LocalDate expiryDate;
    private String certificationNumber;
    private Integer proficiencyScore; // 1-5 scoring
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient fields for display
    private String resourceName;
    private String certificationName;

    public ResourceCertification() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ResourceCertification(Long resourceId, Long certificationId, LocalDate dateObtained, Integer proficiencyScore) {
        this();
        this.resourceId = resourceId;
        this.certificationId = certificationId;
        this.dateObtained = dateObtained;
        this.proficiencyScore = proficiencyScore;
    }

    // Check if certification is expired
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expiryDate);
    }

    // Check if certification is expiring soon (within 30 days)
    public boolean isExpiringSoon() {
        if (expiryDate == null) {
            return false;
        }
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return expiryDate.isBefore(thirtyDaysFromNow) && !isExpired();
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

    public Long getCertificationId() {
        return certificationId;
    }

    public void setCertificationId(Long certificationId) {
        this.certificationId = certificationId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getDateObtained() {
        return dateObtained;
    }

    public void setDateObtained(LocalDate dateObtained) {
        this.dateObtained = dateObtained;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCertificationNumber() {
        return certificationNumber;
    }

    public void setCertificationNumber(String certificationNumber) {
        this.certificationNumber = certificationNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getProficiencyScore() {
        return proficiencyScore;
    }

    public void setProficiencyScore(Integer proficiencyScore) {
        if (proficiencyScore != null && (proficiencyScore < 1 || proficiencyScore > 5)) {
            throw new IllegalArgumentException("Proficiency score must be between 1 and 5");
        }
        this.proficiencyScore = proficiencyScore;
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getCertificationName() {
        return certificationName;
    }

    public void setCertificationName(String certificationName) {
        this.certificationName = certificationName;
    }

    public String getProficiencyDescription() {
        if (proficiencyScore == null) return "Not Rated";
        switch (proficiencyScore) {
            case 1: return "Beginner";
            case 2: return "Basic";
            case 3: return "Intermediate";
            case 4: return "Advanced";
            case 5: return "Expert";
            default: return "Unknown";
        }
    }
}
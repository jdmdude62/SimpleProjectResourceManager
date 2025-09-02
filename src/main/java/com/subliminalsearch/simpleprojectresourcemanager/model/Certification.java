package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDateTime;

public class Certification {
    private Long id;
    private String name;
    private String description;
    private String issuingOrganization;
    private Integer validityPeriodMonths; // How long the certification is valid
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Certification() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Certification(String name, String description, String issuingOrganization) {
        this();
        this.name = name;
        this.description = description;
        this.issuingOrganization = issuingOrganization;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public String getIssuingOrganization() {
        return issuingOrganization;
    }

    public void setIssuingOrganization(String issuingOrganization) {
        this.issuingOrganization = issuingOrganization;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getValidityPeriodMonths() {
        return validityPeriodMonths;
    }

    public void setValidityPeriodMonths(Integer validityPeriodMonths) {
        this.validityPeriodMonths = validityPeriodMonths;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    @Override
    public String toString() {
        return name;
    }
}
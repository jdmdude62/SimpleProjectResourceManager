package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Project {
    private Long id;
    private String projectId;
    private String description;
    private Long projectManagerId; // Reference to project_managers table
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Client contact fields
    private String contactName;
    private String contactEmail;  // Can contain multiple emails separated by semicolons
    private String contactPhone;
    private String contactCompany;
    private String contactRole;
    private boolean sendReports = true;
    private String reportFrequency = "WEEKLY";  // DAILY, WEEKLY, BIWEEKLY, MONTHLY
    private LocalDateTime lastReportSent;
    
    // Budget and financial fields
    private Double budgetAmount;
    private Double actualCost;
    private Double revenueAmount;
    private String currencyCode = "USD";
    private Double laborCost;
    private Double materialCost;
    private Double travelCost;
    private Double otherCost;
    private String costNotes;

    public Project() {
        this.status = ProjectStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Project(String projectId, String description, LocalDate startDate, LocalDate endDate) {
        this();
        this.projectId = projectId;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
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

    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) startDate.until(endDate).getDays() + 1;
    }
    
    // Client contact getters and setters
    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
        this.updatedAt = LocalDateTime.now();
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String[] getContactEmails() {
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            return new String[0];
        }
        return contactEmail.split(";");
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
        this.updatedAt = LocalDateTime.now();
    }

    public String getContactCompany() {
        return contactCompany;
    }

    public void setContactCompany(String contactCompany) {
        this.contactCompany = contactCompany;
        this.updatedAt = LocalDateTime.now();
    }

    public String getContactRole() {
        return contactRole;
    }

    public void setContactRole(String contactRole) {
        this.contactRole = contactRole;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSendReports() {
        return sendReports;
    }

    public void setSendReports(boolean sendReports) {
        this.sendReports = sendReports;
        this.updatedAt = LocalDateTime.now();
    }

    public String getReportFrequency() {
        return reportFrequency;
    }

    public void setReportFrequency(String reportFrequency) {
        this.reportFrequency = reportFrequency;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getLastReportSent() {
        return lastReportSent;
    }

    public void setLastReportSent(LocalDateTime lastReportSent) {
        this.lastReportSent = lastReportSent;
    }

    // Budget and financial getters/setters
    public Double getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Double budgetAmount) {
        this.budgetAmount = budgetAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getActualCost() {
        return actualCost;
    }

    public void setActualCost(Double actualCost) {
        this.actualCost = actualCost;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getRevenueAmount() {
        return revenueAmount;
    }

    public void setRevenueAmount(Double revenueAmount) {
        this.revenueAmount = revenueAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(Double laborCost) {
        this.laborCost = laborCost;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(Double materialCost) {
        this.materialCost = materialCost;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(Double travelCost) {
        this.travelCost = travelCost;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getOtherCost() {
        return otherCost;
    }

    public void setOtherCost(Double otherCost) {
        this.otherCost = otherCost;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCostNotes() {
        return costNotes;
    }

    public void setCostNotes(String costNotes) {
        this.costNotes = costNotes;
        this.updatedAt = LocalDateTime.now();
    }

    // Calculate total cost from components
    public Double getTotalCost() {
        double total = 0.0;
        if (laborCost != null) total += laborCost;
        if (materialCost != null) total += materialCost;
        if (travelCost != null) total += travelCost;
        if (otherCost != null) total += otherCost;
        
        // If we have component costs, use those; otherwise use actualCost if available
        if (total > 0) {
            return total;
        } else if (actualCost != null) {
            return actualCost;
        } else {
            // Return 0 if no cost data is available
            return 0.0;
        }
    }

    // Calculate profit margin
    public Double getProfitMargin() {
        if (revenueAmount != null && revenueAmount > 0) {
            Double cost = getTotalCost();
            if (cost != null && cost >= 0) {
                return ((revenueAmount - cost) / revenueAmount) * 100;
            }
        }
        return null;
    }

    // Calculate budget variance
    public Double getBudgetVariance() {
        if (budgetAmount != null && budgetAmount > 0) {
            Double cost = getTotalCost();
            if (cost != null && cost >= 0) {
                return budgetAmount - cost;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(projectId, project.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId);
    }

    @Override
    public String toString() {
        return String.format("Project{id='%s', description='%s', dates=%s to %s, status=%s}", 
                projectId, description, startDate, endDate, status);
    }
}
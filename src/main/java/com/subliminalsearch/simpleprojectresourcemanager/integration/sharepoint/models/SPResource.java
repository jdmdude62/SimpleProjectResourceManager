package com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.models;

public class SPResource {
    private Integer id;
    private String title;  // Full name
    private String resourceId;
    private String email;
    private String phone;
    private String resourceType;
    private String department;
    private String skills;
    private String domainLogin;  // AD username (domain\\username or username@domain.com)
    private Integer personId;  // SharePoint User ID for People fields
    private String etag;
    
    public SPResource() {
    }
    
    public SPResource(String resourceId, String title, String domainLogin) {
        this.resourceId = resourceId;
        this.title = title;
        this.domainLogin = domainLogin;
    }
    
    // Getters and setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getSkills() {
        return skills;
    }
    
    public void setSkills(String skills) {
        this.skills = skills;
    }
    
    public String getDomainLogin() {
        return domainLogin;
    }
    
    public void setDomainLogin(String domainLogin) {
        this.domainLogin = domainLogin;
    }
    
    public Integer getPersonId() {
        return personId;
    }
    
    public void setPersonId(Integer personId) {
        this.personId = personId;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public void setEtag(String etag) {
        this.etag = etag;
    }
    
    public String getFormattedLogin() {
        if (domainLogin != null && !domainLogin.isEmpty()) {
            // Convert to email format if it's in domain\\user format
            if (domainLogin.contains("\\\\")) {
                String[] parts = domainLogin.split("\\\\\\\\");
                if (parts.length == 2) {
                    return parts[1] + "@" + parts[0] + ".com";
                }
            }
            return domainLogin;
        }
        return email; // Fallback to email if no domain login
    }
}
package com.subliminalsearch.simpleprojectresourcemanager.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Resource {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private ResourceType resourceType;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Resource() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public Resource(String name, String email, ResourceType resourceType) {
        this();
        this.name = name;
        this.email = email;
        this.resourceType = resourceType;
    }

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

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ResourceCategory getCategory() {
        return resourceType != null ? resourceType.getCategory() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(name, resource.name) && Objects.equals(email, resource.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email);
    }

    @Override
    public String toString() {
        return String.format("Resource{name='%s', type=%s, active=%s}", 
                name, resourceType, isActive);
    }
}
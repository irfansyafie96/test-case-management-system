package com.yourproject.tcm.model.dto;

import java.time.LocalDateTime;

public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String organizationName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String createdBy;

    public ProjectDTO(Long id, String name, String description, String organizationName, LocalDateTime createdDate, LocalDateTime updatedDate, String createdBy) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.organizationName = organizationName;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

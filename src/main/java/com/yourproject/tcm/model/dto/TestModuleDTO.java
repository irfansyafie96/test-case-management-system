package com.yourproject.tcm.model.dto;

public class TestModuleDTO {
    private Long id;
    private String name;
    private String description;
    private Long projectId;
    private String projectName;

    public TestModuleDTO(Long id, String name, String description, Long projectId, String projectName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.projectName = projectName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
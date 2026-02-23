package com.yourproject.tcm.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class TestCycleDTO {
    
    private Long id;
    private String name;
    private String description;
    private String redmineProjectUrl;
    private String redmineProjectIdentifier;
    private Long projectId;
    private String projectName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @JsonProperty("isActive")
    private boolean isActive;
    private int sortOrder;
    private LocalDateTime createdDate;
    private String createdBy;
    
    // Execution stats for this cycle
    private long totalExecutions;
    private long passedCount;
    private long failedCount;
    private long pendingCount;
    
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
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRedmineProjectUrl() {
        return redmineProjectUrl;
    }
    
    public void setRedmineProjectUrl(String redmineProjectUrl) {
        this.redmineProjectUrl = redmineProjectUrl;
    }
    
    public String getRedmineProjectIdentifier() {
        return redmineProjectIdentifier;
    }
    
    public void setRedmineProjectIdentifier(String redmineProjectIdentifier) {
        this.redmineProjectIdentifier = redmineProjectIdentifier;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public long getTotalExecutions() {
        return totalExecutions;
    }
    
    public void setTotalExecutions(long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }
    
    public long getPassedCount() {
        return passedCount;
    }
    
    public void setPassedCount(long passedCount) {
        this.passedCount = passedCount;
    }
    
    public long getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }
    
    public long getPendingCount() {
        return pendingCount;
    }
    
    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }
}

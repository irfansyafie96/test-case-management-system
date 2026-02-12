package com.yourproject.tcm.model.dto;

import java.time.LocalDateTime;

/**
 * DTO for Redmine Issue - used for API responses
 */
public class RedmineIssueDTO {
    
    private Long id;
    private String redmineIssueId;
    private String redmineIssueUrl;
    private String bugReportSubject;
    private String bugReportDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public RedmineIssueDTO() {}
    
    public RedmineIssueDTO(Long id, String redmineIssueId, String redmineIssueUrl, 
                           String bugReportSubject, String bugReportDescription,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.redmineIssueId = redmineIssueId;
        this.redmineIssueUrl = redmineIssueUrl;
        this.bugReportSubject = bugReportSubject;
        this.bugReportDescription = bugReportDescription;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRedmineIssueId() {
        return redmineIssueId;
    }
    
    public void setRedmineIssueId(String redmineIssueId) {
        this.redmineIssueId = redmineIssueId;
    }
    
    public String getRedmineIssueUrl() {
        return redmineIssueUrl;
    }
    
    public void setRedmineIssueUrl(String redmineIssueUrl) {
        this.redmineIssueUrl = redmineIssueUrl;
    }
    
    public String getBugReportSubject() {
        return bugReportSubject;
    }
    
    public void setBugReportSubject(String bugReportSubject) {
        this.bugReportSubject = bugReportSubject;
    }
    
    public String getBugReportDescription() {
        return bugReportDescription;
    }
    
    public void setBugReportDescription(String bugReportDescription) {
        this.bugReportDescription = bugReportDescription;
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
}

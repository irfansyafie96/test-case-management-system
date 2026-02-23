package com.yourproject.tcm.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TicketDTO {
    
    private Long id;
    private String redmineIssueId;
    private String redmineIssueUrl;
    private String bugReportSubject;
    private String bugReportDescription;
    private String status;
    private Long executionId;
    private String testCaseTitle;
    private String testCaseId;
    private String projectName;
    private Long projectId;
    private String cycleName;
    private Long cycleId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketAuditLogDTO> auditLogs;
    
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }
    
    public String getTestCaseTitle() {
        return testCaseTitle;
    }
    
    public void setTestCaseTitle(String testCaseTitle) {
        this.testCaseTitle = testCaseTitle;
    }
    
    public String getTestCaseId() {
        return testCaseId;
    }
    
    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getCycleName() {
        return cycleName;
    }
    
    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }
    
    public Long getCycleId() {
        return cycleId;
    }
    
    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    
    public List<TicketAuditLogDTO> getAuditLogs() {
        return auditLogs;
    }
    
    public void setAuditLogs(List<TicketAuditLogDTO> auditLogs) {
        this.auditLogs = auditLogs;
    }
}

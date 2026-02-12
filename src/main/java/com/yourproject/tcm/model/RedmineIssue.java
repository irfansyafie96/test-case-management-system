package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RedmineIssue Entity - Represents a Redmine issue linked to a test execution
 * 
 * Supports multiple issues per test execution, allowing users to track
 * all bugs related to a single failed test case.
 */
@Entity
@Table(name = "redmine_issues")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RedmineIssue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonIgnoreProperties({"stepResults", "testCase"})
    private TestExecution execution;
    
    @Column(name = "redmine_issue_id", length = 100)
    private String redmineIssueId;
    
    @Column(name = "redmine_issue_url", length = 500, nullable = false)
    private String redmineIssueUrl;
    
    @Column(name = "bug_report_subject", length = 500)
    private String bugReportSubject;
    
    @Column(name = "bug_report_description", columnDefinition = "TEXT")
    private String bugReportDescription;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public RedmineIssue() {}
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public TestExecution getExecution() {
        return execution;
    }
    
    public void setExecution(TestExecution execution) {
        this.execution = execution;
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

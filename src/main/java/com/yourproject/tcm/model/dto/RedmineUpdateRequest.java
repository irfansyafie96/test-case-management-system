package com.yourproject.tcm.model.dto;

import jakarta.validation.constraints.Size;

public class RedmineUpdateRequest {
    
    @Size(max = 100, message = "Issue ID must not exceed 100 characters")
    private String redmineIssueId;
    
    @Size(max = 500, message = "Issue URL must not exceed 500 characters")
    private String redmineIssueUrl;
    
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String bugReportSubject;
    
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String bugReportDescription;

    public RedmineUpdateRequest() {}

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
}

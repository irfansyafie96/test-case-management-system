package com.yourproject.tcm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ExecutionCompleteRequest {
    @NotBlank(message = "Overall result is required")
    @Pattern(regexp = "^(PASSED|FAILED|BLOCKED|PARTIALLY_PASSED)$", 
             message = "Overall result must be PASSED, FAILED, BLOCKED, or PARTIALLY_PASSED")
    private String overallResult;
    
    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    // Redmine Integration Fields (Optional - only needed for FAILED results)
    private String bugReportSubject;
    
    private String bugReportDescription;
    
    private String redmineIssueUrl;

    // Constructors
    public ExecutionCompleteRequest() {}

    public ExecutionCompleteRequest(String overallResult, String notes) {
        this.overallResult = overallResult;
        this.notes = notes;
    }

    // Getters and Setters
    public String getOverallResult() {
        return overallResult;
    }

    public void setOverallResult(String overallResult) {
        this.overallResult = overallResult;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getRedmineIssueUrl() {
        return redmineIssueUrl;
    }

    public void setRedmineIssueUrl(String redmineIssueUrl) {
        this.redmineIssueUrl = redmineIssueUrl;
    }
}

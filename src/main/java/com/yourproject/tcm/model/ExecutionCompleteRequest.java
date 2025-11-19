package com.yourproject.tcm.model;

public class ExecutionCompleteRequest {
    private String overallResult;
    private String notes;

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
}

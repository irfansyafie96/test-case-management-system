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

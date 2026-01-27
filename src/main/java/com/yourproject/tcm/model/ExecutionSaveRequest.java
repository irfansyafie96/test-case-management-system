package com.yourproject.tcm.model;

import jakarta.validation.constraints.Size;

/**
 * Request model for saving execution work-in-progress
 * Allows saving notes without requiring an overall result status
 */
public class ExecutionSaveRequest {

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    public ExecutionSaveRequest() {
    }

    public ExecutionSaveRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
package com.yourproject.tcm.model.dto;

import java.util.List;

/**
 * DTO for bulk module assignment requests
 * Used when assigning/removing multiple modules for a user in the project team page
 */
public class BulkAssignmentRequest {
    private List<ModuleAssignmentRequest> assignments;
    private List<ModuleAssignmentRequest> removals;

    // Default constructor
    public BulkAssignmentRequest() {
    }

    // Constructor with parameters
    public BulkAssignmentRequest(List<ModuleAssignmentRequest> assignments, List<ModuleAssignmentRequest> removals) {
        this.assignments = assignments;
        this.removals = removals;
    }

    // Getters and setters
    public List<ModuleAssignmentRequest> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ModuleAssignmentRequest> assignments) {
        this.assignments = assignments;
    }

    public List<ModuleAssignmentRequest> getRemovals() {
        return removals;
    }

    public void setRemovals(List<ModuleAssignmentRequest> removals) {
        this.removals = removals;
    }
}

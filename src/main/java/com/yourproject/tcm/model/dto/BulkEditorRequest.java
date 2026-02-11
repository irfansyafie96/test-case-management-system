package com.yourproject.tcm.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * BulkEditorRequest DTO
 * 
 * Request object for bulk assigning or removing module editors.
 * Used in the Project Team page to manage multiple module editor assignments at once.
 * 
 * Used in:
 * - POST /api/modules/editors/bulk-assign
 */
public class BulkEditorRequest {
    
    private List<ModuleEditorRequest> assignments = new ArrayList<>();
    private List<ModuleEditorRequest> removals = new ArrayList<>();
    
    public BulkEditorRequest() {}
    
    public List<ModuleEditorRequest> getAssignments() {
        return assignments;
    }
    
    public void setAssignments(List<ModuleEditorRequest> assignments) {
        this.assignments = assignments;
    }
    
    public List<ModuleEditorRequest> getRemovals() {
        return removals;
    }
    
    public void setRemovals(List<ModuleEditorRequest> removals) {
        this.removals = removals;
    }
}
package com.yourproject.tcm.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ModuleEditorRequest DTO
 * 
 * Request object for assigning or removing a user as a module editor.
 * Only QA and BA users can be assigned as module editors.
 * 
 * Used in:
 * - POST /api/modules/editors/assign
 * - DELETE /api/modules/editors/assign
 */
public class ModuleEditorRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Test Module ID is required")
    private Long testModuleId;
    
    public ModuleEditorRequest() {}
    
    public ModuleEditorRequest(Long userId, Long testModuleId) {
        this.userId = userId;
        this.testModuleId = testModuleId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getTestModuleId() {
        return testModuleId;
    }
    
    public void setTestModuleId(Long testModuleId) {
        this.testModuleId = testModuleId;
    }
}
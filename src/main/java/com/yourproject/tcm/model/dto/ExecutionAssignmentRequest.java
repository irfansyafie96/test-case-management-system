package com.yourproject.tcm.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ExecutionAssignmentRequest DTO
 * 
 * Request object for assigning or removing a user as an execution assignee.
 * QA, BA, and TESTER users can be assigned as execution assignees.
 * 
 * Used in:
 * - POST /api/modules/execution-assign
 * - DELETE /api/modules/execution-assign
 */
public class ExecutionAssignmentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Test Module ID is required")
    private Long testModuleId;
    
    public ExecutionAssignmentRequest() {}
    
    public ExecutionAssignmentRequest(Long userId, Long testModuleId) {
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
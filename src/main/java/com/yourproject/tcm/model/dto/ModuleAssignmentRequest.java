package com.yourproject.tcm.model.dto;

/**
 * DTO for test module assignment requests
 * Used when assigning or removing TESTER (or QA/BA) users from test modules
 */
public class ModuleAssignmentRequest {
    private Long userId;
    private Long testModuleId;

    // Default constructor
    public ModuleAssignmentRequest() {
    }

    // Constructor with parameters
    public ModuleAssignmentRequest(Long userId, Long testModuleId) {
        this.userId = userId;
        this.testModuleId = testModuleId;
    }

    // Getters and setters
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
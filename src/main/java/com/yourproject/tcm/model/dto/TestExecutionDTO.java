package com.yourproject.tcm.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for TestExecution information
 * Used to safely serialize test execution data without exposing entity relationships
 * or causing circular reference issues during JSON serialization
 */
public class TestExecutionDTO {
    private Long id;
    private String testCaseId;
    private String title;
    private LocalDateTime executionDate;
    private String overallResult;
    private String status;
    private String notes;
    private Long duration;
    private String environment;
    private String executedBy;
    private Long assignedToUserId;
    private String assignedToUsername;
    private List<TestStepResultDTO> stepResults;
    
    // Hierarchy information
    private Long testSuiteId;
    private String testSuiteName;
    private Long moduleId;
    private String moduleName;
    private Long projectId;
    private String projectName;
    
    public TestExecutionDTO() {}
    
    public TestExecutionDTO(Long id, String testCaseId, String title, LocalDateTime executionDate, 
                           String overallResult, String notes, Long duration, String environment,
                           String executedBy, Long assignedToUserId, String assignedToUsername,
                           Long testSuiteId, String testSuiteName, Long moduleId, String moduleName,
                           Long projectId, String projectName, List<TestStepResultDTO> stepResults) {
        this.id = id;
        this.testCaseId = testCaseId;
        this.title = title;
        this.executionDate = executionDate;
        this.overallResult = overallResult;
        this.status = overallResult; // Alias for frontend compatibility
        this.notes = notes;
        this.duration = duration;
        this.environment = environment;
        this.executedBy = executedBy;
        this.assignedToUserId = assignedToUserId;
        this.assignedToUsername = assignedToUsername;
        this.testSuiteId = testSuiteId;
        this.testSuiteName = testSuiteName;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.projectId = projectId;
        this.projectName = projectName;
        this.stepResults = stepResults;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTestCaseId() { return testCaseId; }
    public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public LocalDateTime getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDateTime executionDate) { this.executionDate = executionDate; }
    
    public String getOverallResult() { return overallResult; }
    public void setOverallResult(String overallResult) { 
        this.overallResult = overallResult;
        this.status = overallResult; // Keep status in sync
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.overallResult = status; // Keep overallResult in sync
    }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    
    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    
    public String getAssignedToUsername() { return assignedToUsername; }
    public void setAssignedToUsername(String assignedToUsername) { this.assignedToUsername = assignedToUsername; }
    
    public List<TestStepResultDTO> getStepResults() { return stepResults; }
    public void setStepResults(List<TestStepResultDTO> stepResults) { this.stepResults = stepResults; }
    
    public Long getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(Long testSuiteId) { this.testSuiteId = testSuiteId; }
    
    public String getTestSuiteName() { return testSuiteName; }
    public void setTestSuiteName(String testSuiteName) { this.testSuiteName = testSuiteName; }
    
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }
    
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    /**
     * Inner DTO for TestStepResult
     * Contains the step result data along with the test step action and expected result
     */
    public static class TestStepResultDTO {
        private Long id;
        private Integer stepNumber;
        private String status;
        private String actualResult;
        private String action; // From testStep
        private String expectedResult; // From testStep
        
        public TestStepResultDTO() {}
        
        public TestStepResultDTO(Long id, Integer stepNumber, String status, String actualResult, 
                                 String action, String expectedResult) {
            this.id = id;
            this.stepNumber = stepNumber;
            this.status = status;
            this.actualResult = actualResult;
            this.action = action;
            this.expectedResult = expectedResult;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Integer getStepNumber() { return stepNumber; }
        public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getActualResult() { return actualResult; }
        public void setActualResult(String actualResult) { this.actualResult = actualResult; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    }
}

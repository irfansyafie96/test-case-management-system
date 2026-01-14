package com.yourproject.tcm.model.dto;

public class TestCaseDTO {
    private Long id;
    private String testCaseId;
    private String title;
    private String description;
    private Long testSuiteId;
    private String testSuiteName;
    private String moduleName;
    private String projectName;
    private Integer stepCount;

    public TestCaseDTO(Long id, String testCaseId, String title, String description, 
                       Long testSuiteId, String testSuiteName, String moduleName, 
                       String projectName, Integer stepCount) {
        this.id = id;
        this.testCaseId = testCaseId;
        this.title = title;
        this.description = description;
        this.testSuiteId = testSuiteId;
        this.testSuiteName = testSuiteName;
        this.moduleName = moduleName;
        this.projectName = projectName;
        this.stepCount = stepCount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTestCaseId() { return testCaseId; }
    public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(Long testSuiteId) { this.testSuiteId = testSuiteId; }

    public String getTestSuiteName() { return testSuiteName; }
    public void setTestSuiteName(String testSuiteName) { this.testSuiteName = testSuiteName; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
}
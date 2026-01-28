package com.yourproject.tcm.model.dto;

import java.util.List;

public class TestCaseDTO {
    private Long id;
    private String testCaseId;
    private String title;
    private String description;
    private String scenario;
    private Long testSubmoduleId;
    private String testSubmoduleName;
    private String moduleName;
    private String projectName;
    private Integer stepCount;
    private List<TestStepDTO> testSteps;

    public TestCaseDTO(Long id, String testCaseId, String title, String description, String scenario,
                       Long testSubmoduleId, String testSubmoduleName, String moduleName,
                       String projectName, Integer stepCount, List<TestStepDTO> testSteps) {
        this.id = id;
        this.testCaseId = testCaseId;
        this.title = title;
        this.description = description;
        this.scenario = scenario;
        this.testSubmoduleId = testSubmoduleId;
        this.testSubmoduleName = testSubmoduleName;
        this.moduleName = moduleName;
        this.projectName = projectName;
        this.stepCount = stepCount;
        this.testSteps = testSteps;
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

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public Long getTestSubmoduleId() { return testSubmoduleId; }
    public void setTestSubmoduleId(Long testSubmoduleId) { this.testSubmoduleId = testSubmoduleId; }

    public String getTestSubmoduleName() { return testSubmoduleName; }
    public void setTestSubmoduleName(String testSubmoduleName) { this.testSubmoduleName = testSubmoduleName; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }

    public List<TestStepDTO> getTestSteps() { return testSteps; }
    public void setTestSteps(List<TestStepDTO> testSteps) { this.testSteps = testSteps; }

    // Inner class for test step data
    public static class TestStepDTO {
        private Long id;
        private Integer stepNumber;
        private String action;
        private String expectedResult;

        public TestStepDTO(Long id, Integer stepNumber, String action, String expectedResult) {
            this.id = id;
            this.stepNumber = stepNumber;
            this.action = action;
            this.expectedResult = expectedResult;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getStepNumber() { return stepNumber; }
        public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    }
}
package com.yourproject.tcm.model.dto;

import com.yourproject.tcm.model.TestStep;

public class StepResultResponse {
    private Long id;
    private Long stepId;
    private Integer stepNumber; // Mirrors testStep.stepNumber for ordering
    private String actualResult;
    private String status; // "Pass", "Fail", "Skipped"
    private String action; // Test step action
    private String expectedResult; // Test step expected result

    public StepResultResponse(Long id, Long stepId, Integer stepNumber, String actualResult, String status, String action, String expectedResult) {
        this.id = id;
        this.stepId = stepId;
        this.stepNumber = stepNumber;
        this.actualResult = actualResult;
        this.status = status;
        this.action = action;
        this.expectedResult = expectedResult;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getStepId() {
        return stepId;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public String getActualResult() {
        return actualResult;
    }

    public String getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
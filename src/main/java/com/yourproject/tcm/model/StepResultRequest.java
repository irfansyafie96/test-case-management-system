package com.yourproject.tcm.model;

public class StepResultRequest {
    private String status;
    private String actualResult;

    // Constructors
    public StepResultRequest() {}

    public StepResultRequest(String status, String actualResult) {
        this.status = status;
        this.actualResult = actualResult;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }
}

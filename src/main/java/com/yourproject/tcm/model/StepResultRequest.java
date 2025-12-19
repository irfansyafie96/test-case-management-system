package com.yourproject.tcm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StepResultRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PASSED|FAILED|BLOCKED|SKIPPED)$", message = "Status must be PASSED, FAILED, BLOCKED, or SKIPPED")
    private String status;
    
    @Size(max = 2000, message = "Actual result must not exceed 2000 characters")
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

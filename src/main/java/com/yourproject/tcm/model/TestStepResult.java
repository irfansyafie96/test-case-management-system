package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "test_step_results")
public class TestStepResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonBackReference
    private TestExecution testExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private TestStep testStep;

    @Column(nullable = false)
    private Integer stepNumber; // Mirrors testStep.stepNumber for ordering

    @Column(columnDefinition = "TEXT")
    private String actualResult;

    @Column(nullable = false)
    private String status; // "Pass", "Fail", "Skipped"

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TestExecution getTestExecution() {
        return testExecution;
    }

    public void setTestExecution(TestExecution testExecution) {
        this.testExecution = testExecution;
    }

    public TestStep getTestStep() {
        return testStep;
    }

    public void setTestStep(TestStep testStep) {
        this.testStep = testStep;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

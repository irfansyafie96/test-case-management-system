package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "test_steps")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepNumber; // 1, 2, 3...

    @Column(columnDefinition = "TEXT", nullable = false)
    private String action; // "Steps" from your Excel

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedResult; // "Expected Result" from your Excel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @JsonBackReference // This should match with JsonManagedReference in TestCase
    @JsonIgnoreProperties({"testSteps"}) // Prevent circular reference back to TestCase
    private TestCase testCase;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }
}
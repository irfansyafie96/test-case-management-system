package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * TestStepResult Entity - Represents the result of executing one step in a test
 *
 * When a test case is executed (TestExecution), each step of that test case
 * gets a TestStepResult record that captures:
 * - Which execution this result belongs to
 * - Which step was executed
 * - What the actual result was when executed
 * - What the status is (Pass/Fail/Skipped)
 *
 * This is the most granular level of tracking in the system. You can see
 * exactly which steps passed or failed in each execution of a test case.
 *
 * Relationship Structure:
 * - Many TestStepResults belong to One TestExecution (ManyToOne)
 * - Many TestStepResults reference One TestStep (ManyToOne)
 */
@Entity
@Table(name = "test_step_results")  // Maps to 'test_step_results' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestStepResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for this step result

    /**
     * Many-to-One relationship: Many TestStepResults belong to One TestExecution
     * fetch = FetchType.LAZY: Only load execution data when explicitly accessed
     * @JoinColumn: Foreign key 'execution_id' in test_step_results table points to TestExecution
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestExecutionId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestExecution testExecution;  // The execution this step result belongs to

    /**
     * Many-to-One relationship: Many TestStepResults reference One TestStep
     * fetch = FetchType.LAZY: Only load test step data when explicitly accessed
     * @JoinColumn: Foreign key 'step_id' in test_step_results table points to TestStep
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestStepId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestStep testStep;  // The test step that was executed

    @Column(nullable = false)  // Step number is required
    private Integer stepNumber; // Copy of testStep.stepNumber to maintain order during execution

    @Column(columnDefinition = "TEXT")  // TEXT allows longer actual results
    private String actualResult;  // What actually happened when this step was executed

    @Column(nullable = false)  // Status is required
    private String status; // Result status: "Pass", "Fail", "Skipped", etc.

    // Getters and Setters - Standard methods to access private fields
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

    /**
     * Get the ID of the test execution this step result belongs to.
     * This is used by Jackson when serializing, since the testExecution field
     * is marked with @JsonIgnore to prevent Hibernate proxy serialization.
     */
    public Long getTestExecutionId() {
        return testExecution != null ? testExecution.getId() : null;
    }

    /**
     * Get the ID of the test step this result is for.
     * This is used by Jackson when serializing, since the testStep field
     * is marked with @JsonIgnore to prevent Hibernate proxy serialization.
     */
    public Long getTestStepId() {
        return testStep != null ? testStep.getId() : null;
    }
}

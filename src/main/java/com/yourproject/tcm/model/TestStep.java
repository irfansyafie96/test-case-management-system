package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * TestStep Entity - Fifth and final level in the test case hierarchy
 *
 * TestStep represents a single action in a test case with what should happen
 * and what result is expected. Each test case has one or more steps.
 *
 * For example, if TestCase is "Register New Training Provider", then TestStep
 * could be "Step 1: Navigate to Registration Page" with expected result
 * "Registration page loads successfully".
 *
 * Relationship Structure:
 * - Many TestSteps belong to One TestCase (ManyToOne)
 *
 * This is the bottom level of the test case hierarchy. When test cases
 * are executed, each step gets a result recorded in TestStepResult entities.
 */
@Entity
@Table(name = "test_steps")  // Maps to 'test_steps' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for the test step

    @Column(nullable = false)  // Step number is required
    private Integer stepNumber; // Step number in sequence (1, 2, 3, etc.)

    @Column(columnDefinition = "TEXT", nullable = false)  // TEXT allows longer content
    private String action; // The action to perform in this step (e.g., "Click Register Button")

    @Column(columnDefinition = "TEXT", nullable = false)  // TEXT allows longer content
    private String expectedResult; // What should happen (e.g., "Registration form appears")

    /**
     * Many-to-One relationship: Many TestSteps belong to One TestCase
     * fetch = FetchType.LAZY: Only load test case data when explicitly accessed
     * @JoinColumn: Foreign key 'test_case_id' in test_steps table points to TestCase
     * @JsonBackReference: Part of bidirectional relationship, prevents JSON loops
     */
    @ManyToOne(fetch = FetchType.LAZY)  // Many steps can belong to one test case
    @JoinColumn(name = "test_case_id", nullable = false)  // Foreign key column
    @JsonBackReference // Completes the bidirectional relationship with TestCase
    private TestCase testCase;  // The test case this step belongs to

    // Getters and Setters - Standard methods to access private fields
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
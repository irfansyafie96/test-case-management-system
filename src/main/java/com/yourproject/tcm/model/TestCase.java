package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.List;

/**
 * TestCase Entity - Fourth level in the test case hierarchy
 *
 * TestCase represents a specific test scenario with steps to execute.
 * For example, if TestSuite is "TP Registration", then TestCase could be
 * "TRM-TS-01 Register New Training Provider with Valid Details".
 *
 * Each test case contains multiple TestSteps that define what actions to take
 * and what results to expect. Test cases can be executed multiple times,
 * creating TestExecution records with their results.
 *
 * Relationship Structure:
 * - Many TestCases belong to One TestSuite (ManyToOne)
 * - One TestCase contains Many TestSteps (OneToMany)
 * - One TestCase can have Many TestExecutions (OneToMany, defined in TestExecution)
 */
@Entity
@Table(name = "test_cases")  // Maps to 'test_cases' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for the test case

    @Column(nullable = false)  // Test case ID is required
    private String testCaseId; // Business ID for the test case (e.g., "TRM-TS-01", "NCS-TC-001")

    @Column(nullable = false)  // Title is required
    private String title; // Title/description of the test case (e.g., "Register New Training Provider")

    @Column(columnDefinition = "TEXT")
    private String description; // Detailed description of the test case

    /**
     * Many-to-One relationship: Many TestCases belong to One TestSuite
     * fetch = FetchType.LAZY: Only load suite data when explicitly accessed
     * @JoinColumn: Foreign key 'test_suite_id' in test_cases table points to TestSuite
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestSuiteId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)  // Many test cases can belong to one suite
    @JoinColumn(name = "test_suite_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestSuite testSuite;  // The suite this test case belongs to

    /**
     * One-to-Many relationship: One TestCase can have Many TestSteps
     * cascade = CascadeType.ALL: Changes to test case cascade to its steps
     * orphanRemoval = true: If a step is removed from this list, it's deleted
     * fetch = FetchType.LAZY: Load steps only when explicitly accessed (improves performance)
     * @OrderBy("stepNumber ASC"): Always keep test steps in ascending order by step number
     * @JsonIgnoreProperties: Prevent circular reference back to TestCase
     */
    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepNumber ASC") // Always keep steps in sequential order (1, 2, 3, etc.)
    @JsonIgnoreProperties({"testCase"})  // Prevent circular reference back to TestCase
    private List<TestStep> testSteps;  // List of steps that make up this test case

    // Getters and Setters - Standard methods to access private fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public List<TestStep> getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(List<TestStep> testSteps) {
        this.testSteps = testSteps;
    }

    /**
     * Get the ID of the test suite this test case belongs to.
     * This is used by Jackson when serializing, since the testSuite field
     * is marked with @JsonIgnore to prevent Hibernate proxy serialization.
     */
    public Long getTestSuiteId() {
        return testSuite != null ? testSuite.getId() : null;
    }
}
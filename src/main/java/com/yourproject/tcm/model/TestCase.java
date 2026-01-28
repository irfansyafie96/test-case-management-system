package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.List;

/**
 * TestCase Entity - Fourth level in the test case hierarchy
 *
 * TestCase represents a specific test scenario with steps to execute.
 * For example, if TestSubmodule is "TP Registration", then TestCase could be
 * "TRM-TS-01 Register New Training Provider with Valid Details".
 *
 * Each test case contains multiple TestSteps that define what actions to take
 * and what results to expect. Test cases can be executed multiple times,
 * creating TestExecution records with their results.
 *
 * Relationship Structure:
 * - Many TestCases belong to One TestSubmodule (ManyToOne)
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

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(columnDefinition = "TEXT")
    private String expectedResult;

    private String tags;

    /**
     * Many-to-One relationship: Many TestCases belong to One TestSubmodule
     * fetch = FetchType.LAZY: Only load submodule data when explicitly accessed
     * @JoinColumn: Foreign key 'test_submodule_id' in test_cases table points to TestSubmodule
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestSubmoduleId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)  // Many test cases can belong to one submodule
    @JoinColumn(name = "test_submodule_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestSubmodule testSubmodule;  // The submodule this test case belongs to

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

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TestExecution> testExecutions;

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

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public TestSubmodule getTestSubmodule() {
        return testSubmodule;
    }

    public void setTestSubmodule(TestSubmodule testSubmodule) {
        this.testSubmodule = testSubmodule;
    }

    public List<TestStep> getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(List<TestStep> testSteps) {
        this.testSteps = testSteps;
    }

    public List<TestExecution> getTestExecutions() {
        return testExecutions;
    }

    public void setTestExecutions(List<TestExecution> testExecutions) {
        this.testExecutions = testExecutions;
    }

    /**
     * Get the ID of the test submodule this test case belongs to.
     * This is used by Jackson when serializing, since the testSubmodule field
     * is marked with @JsonIgnore to prevent Hibernate proxy serialization.
     */
    public Long getTestSubmoduleId() {
        return testSubmodule != null ? testSubmodule.getId() : null;
    }

    public TestModule getTestModule() {
        return testSubmodule != null ? testSubmodule.getTestModule() : null;
    }

    // Flattened hierarchy getters for Frontend (avoids JsonIgnore/Proxy issues)

    public String getTestSubmoduleName() {
        return testSubmodule != null ? testSubmodule.getName() : null;
    }

    public String getModuleName() {
        if (testSubmodule != null && testSubmodule.getTestModule() != null) {
            return testSubmodule.getTestModule().getName();
        }
        return null;
    }

    public String getProjectName() {
        if (testSubmodule != null &&
            testSubmodule.getTestModule() != null &&
            testSubmodule.getTestModule().getProject() != null) {
            return testSubmodule.getTestModule().getProject().getName();
        }
        return null;
    }
}
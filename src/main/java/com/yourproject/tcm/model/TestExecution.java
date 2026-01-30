package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TestExecution Entity - Represents one execution/run of a test case
 *
 * When a user runs a test case to verify if it works, a TestExecution record
 * is created. This entity captures:
 * - Which test case was executed
 * - When it was executed
 * - What the overall result was (Pass/Fail/Incomplete)
 * - Notes about the execution
 * - Results for each individual step (TestStepResult)
 *
 * Key Concept: One test case can be executed multiple times, each creating
 * a separate TestExecution record. This allows tracking the history of
 * test case execution results over time.
 *
 * Relationship Structure:
 * - Many TestExecutions can belong to One TestCase (ManyToOne)
 * - One TestExecution contains Many TestStepResults (OneToMany)
 */
@Entity
@Table(name = "test_executions")  // Maps to 'test_executions' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for this test execution

    /**
     * Many-to-One relationship: Many TestExecutions can belong to One TestCase
     * fetch = FetchType.LAZY: Only load test case data when explicitly accessed
     * @JoinColumn: Foreign key 'test_case_id' in test_executions table points to TestCase
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestCaseId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestCase testCase;  // The test case that was executed

    @Column(nullable = false)  // Execution date is required
    private LocalDateTime executionDate;  // When this test was executed (timestamp)

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(nullable = false)  // Overall result is required
    private String overallResult; // Overall result: "Pass", "Fail", "Incomplete", etc.

    // Separate status field to track execution state without overwriting overallResult
    @Column(name = "execution_status", nullable = false)
    private String status = "PENDING"; // Execution status: "PENDING", "IN_PROGRESS", "COMPLETED"

    // Alias for frontend compatibility - returns overallResult for backward compatibility
    public String getResultStatus() {
        return overallResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Column(columnDefinition = "TEXT")  // TEXT allows longer notes
    private String notes;  // Optional notes about this execution

    // Additional fields for frontend compatibility
    @Column(name = "execution_duration")
    private Long duration;  // Execution duration in milliseconds

    @Column(name = "execution_environment")
    private String environment;  // Environment where test was executed (dev, staging, prod)

    /**
     * Many-to-One relationship: Many TestExecutions can be assigned to One User
     * fetch = FetchType.LAZY: Only load user data when explicitly accessed
     * @JoinColumn: Foreign key 'assigned_to_user_id' in test_executions table points to User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")  // Foreign key column for assigned user
    @JsonIgnoreProperties({"testExecutions", "roles"}) // Prevent circular reference back to TestExecutions
    private User assignedToUser;  // The user assigned to execute this test

    /**
     * One-to-Many relationship: One TestExecution can have Many TestStepResults
     * cascade = CascadeType.ALL: Changes to execution cascade to its step results
     * orphanRemoval = true: If a step result is removed from this list, it's deleted
     * @OrderBy("stepNumber ASC"): Always keep step results in ascending order by step number
     * @JsonIgnoreProperties: Prevent circular reference back to TestExecution
     */
    @OneToMany(mappedBy = "testExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")  // Keep step results in sequential order (1, 2, 3, etc.)
    @JsonIgnoreProperties({"testExecution"}) // Prevent circular reference back to TestExecution
    private List<TestStepResult> stepResults;  // Results for each step in this execution

    // Getters and Setters - Standard methods to access private fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    // Additional getters for frontend compatibility
    public String getTestCaseId() {
        return testCase != null ? testCase.getTestCaseId() : null;
    }

    public String getExecutedBy() {
        return assignedToUser != null ? assignedToUser.getUsername() : null;
    }

    public String getTitle() {
        return testCase != null ? testCase.getTitle() : null;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }

    public String getOverallResult() {
        return overallResult;
    }

    public void setOverallResult(String overallResult) {
        this.overallResult = overallResult;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Getters and setters for new fields
    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public User getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(User assignedToUser) {
        this.assignedToUser = assignedToUser;
    }

    public List<TestStepResult> getStepResults() {
        return stepResults;
    }

    public void setStepResults(List<TestStepResult> stepResults) {
        this.stepResults = stepResults;
    }

    // Helper methods to flatten the hierarchy for the frontend
    // This allows the frontend to group executions without needing the full object graph

    public String getSubmoduleName() {
        if (testCase != null && testCase.getSubmodule() != null) {
            return testCase.getSubmodule().getName();
        }
        return null;
    }

    public Long getSubmoduleId() {
        if (testCase != null && testCase.getSubmodule() != null) {
            return testCase.getSubmodule().getId();
        }
        return null;
    }

    public String getModuleName() {
        if (testCase != null && 
            testCase.getSubmodule() != null && 
            testCase.getSubmodule().getTestModule() != null) {
            return testCase.getSubmodule().getTestModule().getName();
        }
        return null;
    }

    public Long getModuleId() {
        if (testCase != null && 
            testCase.getSubmodule() != null && 
            testCase.getSubmodule().getTestModule() != null) {
            return testCase.getSubmodule().getTestModule().getId();
        }
        return null;
    }

    public String getProjectName() {
        if (testCase != null && 
            testCase.getSubmodule() != null && 
            testCase.getSubmodule().getTestModule() != null &&
            testCase.getSubmodule().getTestModule().getProject() != null) {
            return testCase.getSubmodule().getTestModule().getProject().getName();
        }
        return null;
    }

    public Long getProjectId() {
        if (testCase != null && 
            testCase.getSubmodule() != null && 
            testCase.getSubmodule().getTestModule() != null &&
            testCase.getSubmodule().getTestModule().getProject() != null) {
            return testCase.getSubmodule().getTestModule().getProject().getId();
        }
        return null;
    }
}
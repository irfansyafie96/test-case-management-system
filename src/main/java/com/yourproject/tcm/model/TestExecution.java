package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_executions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @JsonIgnoreProperties({"testSteps"}) // Prevent circular reference back to TestSteps
    private TestCase testCase;

    @Column(nullable = false)
    private LocalDateTime executionDate;

    @Column(nullable = false)
    private String overallResult; // "Pass", "Fail", "Incomplete"

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "testExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @JsonManagedReference
    @JsonIgnoreProperties({"testExecution"}) // Prevent circular reference back to TestExecution
    private List<TestStepResult> stepResults;

    // Getters and Setters
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

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
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

    public List<TestStepResult> getStepResults() {
        return stepResults;
    }

    public void setStepResults(List<TestStepResult> stepResults) {
        this.stepResults = stepResults;
    }
}

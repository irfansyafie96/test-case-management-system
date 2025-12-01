package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "test_cases")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testCaseId; // e.g., "TRM-TS-01"

    @Column(nullable = false)
    private String title; // "Scenario:" from your Excel

    private String priority; // "High", "Medium", "Low"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_suite_id", nullable = false)
    @JsonBackReference
    private TestSuite testSuite;

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC") // Always keep steps in order
    @JsonManagedReference
    private List<TestStep> testSteps;

    // Getters and Setters
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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
}
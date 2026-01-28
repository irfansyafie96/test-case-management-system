package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.List;

/**
 * TestSubmodule Entity - Third level in the test case hierarchy
 *
 * TestSubmodule represents a group of related test cases that test a specific feature
 * or functionality. For example, if TestModule is "Training Market", then TestSubmodule
 * could be "TP Registration", "TP Approval", "TP Training Management", etc.
 *
 * Relationship Structure:
 * - Many TestSubmodules belong to One TestModule (ManyToOne)
 * - One TestSubmodule contains Many TestCases (OneToMany)
 * - fetch = FetchType.EAGER for testCases: Load test cases immediately when submodule is loaded
 *
 * This creates the hierarchical structure: Project → TestModule → TestSubmodule → TestCase
 */
@Entity
@Table(name = "test_submodules")  // Maps to 'test_submodules' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestSubmodule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for the test submodule

    @Column(nullable = false)  // Name is required
    private String name; // Submodule name (e.g., "TP Registration", "User Login")

    /**
     * Many-to-One relationship: Many TestSubmodules belong to One TestModule
     * fetch = FetchType.LAZY: Only load module data when explicitly accessed
     * @JoinColumn: Foreign key 'test_module_id' in test_submodules table points to TestModule
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getTestModuleId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)  // Many submodules can belong to one module
    @JoinColumn(name = "test_module_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private TestModule testModule;  // The module this submodule belongs to

    /**
     * One-to-Many relationship: One TestSubmodule can have Many TestCases
     * cascade = CascadeType.ALL: Changes to submodule cascade to its test cases
     * orphanRemoval = true: If a test case is removed from this list, it's deleted
     * fetch = FetchType.EAGER: Always load test cases when loading the submodule
     * @JsonIgnoreProperties: Prevent circular reference back to TestSubmodule
     */
    @OneToMany(mappedBy = "testSubmodule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"testSubmodule"})  // Prevent circular reference back to TestSubmodule
    private List<TestCase> testCases;  // List of test cases in this submodule

    // Getters and Setters - Standard methods to access private fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestModule getTestModule() {
        return testModule;
    }

    public void setTestModule(TestModule testModule) {
        this.testModule = testModule;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    /**
     * Get the ID of the test module this submodule belongs to.
     * This is used by Jackson when serializing, since the testModule field
     * is marked with @JsonIgnore to prevent Hibernate proxy serialization.
     */
    public Long getTestModuleId() {
        return testModule != null ? testModule.getId() : null;
    }
}
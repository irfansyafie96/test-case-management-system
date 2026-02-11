package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ExecutionAssignee Entity
 * 
 * Represents the assignment of a user (QA, BA, or TESTER) to a test module for execution purposes.
 * When a user is assigned as an execution assignee, test executions are automatically generated
 * for all test cases in the module, with the assigned user set as the executor.
 * 
 * All user roles can be assigned as execution assignees.
 */
@Entity
@Table(name = "execution_assignees")
public class ExecutionAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_module_id", nullable = false)
    @JsonIgnore
    private TestModule testModule;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private String assignedBy;

    // Default constructor
    public ExecutionAssignee() {
        this.assignedAt = LocalDateTime.now();
    }

    // Constructor with user and testModule
    public ExecutionAssignee(User user, TestModule testModule) {
        this();
        this.user = user;
        this.testModule = testModule;
    }

    // Constructor with user, testModule, and assignedBy
    public ExecutionAssignee(User user, TestModule testModule, String assignedBy) {
        this(user, testModule);
        this.assignedBy = assignedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TestModule getTestModule() {
        return testModule;
    }

    public void setTestModule(TestModule testModule) {
        this.testModule = testModule;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    // Equals and HashCode based on user and testModule combination
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionAssignee that = (ExecutionAssignee) o;
        return Objects.equals(user, that.user) && 
               Objects.equals(testModule, that.testModule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, testModule);
    }

    @Override
    public String toString() {
        return "ExecutionAssignee{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", testModule=" + (testModule != null ? testModule.getName() : "null") +
                ", assignedAt=" + assignedAt +
                ", assignedBy='" + assignedBy + '\'' +
                '}';
    }
}

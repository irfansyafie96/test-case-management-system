package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Project Entity - Top-level container in the test case hierarchy
 *
 * This represents a major project in the TCM system (e.g., "NCS Project").
 * Each project can contain multiple test modules, which in turn contain
 * test suites, test cases, etc. This is the root of the hierarchical structure:
 *
 * Project → TestModule → TestSuite → TestCase → TestStep
 *
 * Key Features:
 * - Each project has a unique name (enforced by database constraint)
 * - Projects contain multiple test modules (OneToMany relationship)
 * - When a project is deleted, all its modules and their contents are also deleted (cascade)
 */
@Entity
@Table(name = "projects")  // Maps to 'projects' table in database
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for the project

    @Column(nullable = false, unique = true)  // Name is required and must be unique
    private String name; // Project name (e.g., "NCS", "Training Platform")

    private String description;  // Optional description of the project

    // Additional fields for frontend compatibility
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "created_by")
    private String createdBy;

    /**
     * One-to-Many relationship: One Project can have Many TestModules
     * cascade = CascadeType.ALL: Any changes to project cascade to its modules
     * orphanRemoval = true: If a module is removed from this list, it's deleted from DB
     * mappedBy = "project": The 'project' field in TestModule entity owns this relationship
     * @JsonManagedReference: Prevents infinite loops when serializing to JSON
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TestModule> modules;  // List of modules belonging to this project

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TestModule> getModules() {
        return modules;
    }

    public void setModules(List<TestModule> modules) {
        this.modules = modules;
    }

    // Getters and setters for additional fields
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
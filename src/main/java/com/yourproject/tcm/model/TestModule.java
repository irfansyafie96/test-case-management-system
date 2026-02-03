package com.yourproject.tcm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TestModule Entity - Second level in the test case hierarchy
 *
 * TestModule represents a functional area or module within a project.
 * For example, if Project is "NCS", then TestModule could be "Training Market",
 * "User Management", "Reporting", etc.
 *
 * Relationship Structure:
 * - Many TestModules belong to One Project (ManyToOne)
 * - One TestModule contains Many Submodules (OneToMany)
 *
 * This creates the hierarchical structure: Project → TestModule → Submodule → TestCase
 */
@Entity
@Table(name = "test_modules")  // Maps to 'test_modules' table in database
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Ignore Hibernate proxy properties during JSON serialization
public class TestModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;  // Unique identifier for the test module

    @Column(nullable = false)  // Name is required
    private String name; // Module name (e.g., "Training Market", "User Management")

    @Column(length = 1000)  // Description can be up to 1000 characters
    private String description;  // Optional detailed description

    /**
     * Many-to-One relationship: Many TestModules belong to One Project
     * fetch = FetchType.LAZY: Only load module data when explicitly accessed
     * @JoinColumn: Foreign key 'project_id' in test_modules table points to Project
     * @JsonIgnore: Prevent serialization of Hibernate proxy, use getProjectId() instead
     */
    @ManyToOne(fetch = FetchType.LAZY)  // Many modules can belong to one project
    @JoinColumn(name = "project_id", nullable = false)  // Foreign key column
    @JsonIgnore
    private Project project;  // The project this module belongs to

    /**
     * One-to-Many relationship: One TestModule can have Many Submodules
     * cascade = CascadeType.ALL: Changes to module cascade to its submodules
     * orphanRemoval = true: If a submodule is removed from this list, it's deleted
     * @JsonIgnoreProperties: Prevent circular reference back to TestModule
     */
    @OneToMany(mappedBy = "testModule", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"testModule"})  // Prevent circular reference back to TestModule
    private List<Submodule> submodules;  // List of submodules in this module

    /**
     * Many-to-Many relationship with User entity (module assignments)
     * TESTER users can be assigned to this module to test it
     * QA/BA users can also be assigned modules for testing purposes
     * This is the inverse side of the relationship mapped in User entity
     */
    @ManyToMany(mappedBy = "assignedTestModules", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> assignedUsers = new HashSet<>();  // Users assigned to this module

    /**

     * Transient field to indicate if the current user can edit this module.

     * Not persisted to database, set by controller based on user permissions.

     * Used by frontend to show/hide edit buttons.

     */

    @Transient

    @JsonProperty("isEditable")

    private boolean isEditable;  // UI flag: true if current user can edit this module

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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<Submodule> getSubmodules() {
        return submodules;
    }

    public void setSubmodules(List<Submodule> submodules) {
        this.submodules = submodules;
    }

    public Set<User> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(Set<User> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    @JsonProperty("isEditable")
    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    // Helper method to expose project ID to frontend without exposing entire project object
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    // Helper method to expose project name to frontend for display
    public String getProjectName() {
        return project != null ? project.getName() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestModule that = (TestModule) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
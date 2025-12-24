package com.yourproject.tcm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity - Represents a system user in the Test Case Management System
 *
 * This entity maps to the 'users' table in the database and represents
 * users who can log into the TCM system. Each user has:
 * - Basic account information (username, email, password)
 * - Account status (enabled/disabled)
 * - Creation timestamp
 * - Associated roles (ADMIN, QA, BA, TESTER)
 *
 * The system enforces unique constraint on both username and email
 * to prevent duplicate accounts.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),  // Prevent duplicate usernames
    @UniqueConstraint(columnNames = "email")      // Prevent duplicate emails
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment ID
    private Long id;

    private String username;  // Login username
    private String email;     // User's email address
    private String password;  // Encrypted password

    @Column(name = "enabled")
    private boolean enabled = true;  // Account status - true means active

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // When the account was created

    /**
     * One-to-Many relationship with TestExecution entity (assigned to user)
     * A user can be assigned to multiple test executions
     */
    @OneToMany(mappedBy = "assignedToUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestExecution> testExecutions = new HashSet<>();  // Test executions assigned to this user

    /**
     * Many-to-Many relationship with Role entity
     * A user can have multiple roles, and roles can be assigned to multiple users
     * This creates a 'user_roles' junction table in the database
     */
    @ManyToMany(fetch = FetchType.LAZY)  // Load roles only when accessed
    @JoinTable(name = "user_roles",      // Junction table name
        joinColumns = @JoinColumn(name = "user_id"),      // Column for user ID
        inverseJoinColumns = @JoinColumn(name = "role_id")) // Column for role ID
    private Set<Role> roles = new HashSet<>();  // Set of user's roles

    /**
     * Many-to-Many relationship with Project entity (project assignments)
     * QA/BA users can be assigned to specific projects they can work on
     * This creates a 'user_projects' junction table in the database
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_projects",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "project_id"))
    private Set<Project> assignedProjects = new HashSet<>();  // Projects assigned to this user

    /**
     * Many-to-Many relationship with TestModule entity (module assignments)
     * TESTER users can be assigned to specific modules they can test
     * QA/BA users can also be assigned modules for testing purposes
     * This creates a 'user_test_modules' junction table in the database
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_test_modules",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "test_module_id"))
    private Set<TestModule> assignedTestModules = new HashSet<>();  // Test modules assigned to this user

    /**
     * Default constructor - Creates a user with current timestamp
     */
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor with basic user information
     * @param username User's login name
     * @param email User's email address
     * @param password User's password (will be encrypted before saving)
     */
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = true;              // New users are enabled by default
        this.createdAt = LocalDateTime.now(); // Set creation time
    }

    // Getters and setters - Standard methods to access private fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Alias for frontend compatibility
    public LocalDateTime getCreatedDate() {
        return createdAt;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdAt = createdDate;
    }

    public Set<TestExecution> getTestExecutions() {
        return testExecutions;
    }

    public void setTestExecutions(Set<TestExecution> testExecutions) {
        this.testExecutions = testExecutions;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<Project> getAssignedProjects() {
        return assignedProjects;
    }

    public void setAssignedProjects(Set<Project> assignedProjects) {
        this.assignedProjects = assignedProjects;
    }

    public Set<TestModule> getAssignedTestModules() {
        return assignedTestModules;
    }

    public void setAssignedTestModules(Set<TestModule> assignedTestModules) {
        this.assignedTestModules = assignedTestModules;
    }
}

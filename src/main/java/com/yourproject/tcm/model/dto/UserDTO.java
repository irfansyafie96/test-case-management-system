package com.yourproject.tcm.model.dto;

import java.util.List;

/**
 * Data Transfer Object for User information
 * Used to safely serialize user data without exposing sensitive information
 * or causing circular reference issues during JSON serialization
 */
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String organizationName;
    private List<String> roles;
    private boolean isExternal;
    private List<TestModuleDTO> assignedModulesForEditing;
    private List<TestModuleDTO> assignedModulesForExecution;

    public UserDTO() {}

    public UserDTO(Long id, String username, String email, String organizationName, List<String> roles) {
        this(id, username, email, organizationName, roles, false);
    }

    public UserDTO(Long id, String username, String email, String organizationName, List<String> roles, boolean isExternal) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.organizationName = organizationName;
        this.roles = roles;
        this.isExternal = isExternal;
    }

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

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }

    public List<TestModuleDTO> getAssignedModulesForEditing() {
        return assignedModulesForEditing;
    }

    public void setAssignedModulesForEditing(List<TestModuleDTO> assignedModulesForEditing) {
        this.assignedModulesForEditing = assignedModulesForEditing;
    }

    public List<TestModuleDTO> getAssignedModulesForExecution() {
        return assignedModulesForExecution;
    }

    public void setAssignedModulesForExecution(List<TestModuleDTO> assignedModulesForExecution) {
        this.assignedModulesForExecution = assignedModulesForExecution;
    }
}
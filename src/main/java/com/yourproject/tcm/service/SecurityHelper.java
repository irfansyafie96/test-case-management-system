package com.yourproject.tcm.service;

import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.User;
import org.springframework.stereotype.Service;

/**
 * Helper service for centralized security and permission checks.
 * Reduces code duplication by providing common security check methods.
 */
@Service
public class SecurityHelper {

    private final UserContextService userContextService;

    public SecurityHelper(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    /**
     * Require that the user has ADMIN role.
     * @param currentUser the user to check
     * @throws RuntimeException if user is not an ADMIN
     */
    public void requireAdmin(User currentUser) {
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can perform this action");
        }
    }

    /**
     * Require that the user has ADMIN, QA, or BA role.
     * @param currentUser the user to check
     * @throws RuntimeException if user doesn't have required role
     */
    public void requireAdminQaOrBa(User currentUser) {
        if (!userContextService.isAdmin(currentUser) && 
            !userContextService.isQaOrBa(currentUser)) {
            throw new RuntimeException("Only ADMIN, QA, or BA users can perform this action");
        }
    }

    /**
     * Require that the entity belongs to the same organization as the user.
     * @param currentUser the current user
     * @param entityOrg the organization of the entity being accessed
     * @throws RuntimeException if organizations don't match
     */
    public void requireSameOrganization(User currentUser, Organization entityOrg) {
        if (entityOrg == null || currentUser == null) {
            throw new RuntimeException("Organization not found");
        }
        if (!entityOrg.getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Resource not found or access denied");
        }
    }

    /**
     * Require that the user can access the project.
     * ADMIN users can access any project in their organization.
     * Non-ADMIN users must be assigned to the project or any of its test modules.
     * @param currentUser the current user
     * @param project the project to check access for
     * @throws RuntimeException if user doesn't have access
     */
    public void requireProjectAccess(User currentUser, Project project) {
        if (currentUser == null || project == null) {
            throw new RuntimeException("Project not found");
        }
        
        // Check organization boundary
        requireSameOrganization(currentUser, project.getOrganization());
        
        // ADMIN users can access any project
        if (userContextService.isAdmin(currentUser)) {
            return;
        }
        
        // Non-ADMIN users must be assigned to the project or its modules
        boolean isAssignedToProject = currentUser.getAssignedProjects() != null && 
            currentUser.getAssignedProjects().contains(project);
        
        boolean isAssignedToModule = currentUser.getAssignedTestModules() != null &&
            currentUser.getAssignedTestModules().stream()
                .anyMatch(m -> m.getProject().getId().equals(project.getId()));
        
        if (!isAssignedToProject && !isAssignedToModule) {
            throw new RuntimeException("You are not assigned to this project");
        }
    }

    /**
     * Require that the user can access the test module.
     * ADMIN users can access any module in their organization.
     * Non-ADMIN users must be assigned to the module.
     * @param currentUser the current user
     * @param testModule the test module to check access for
     * @throws RuntimeException if user doesn't have access
     */
    public void requireModuleAccess(User currentUser, TestModule testModule) {
        if (currentUser == null || testModule == null || testModule.getProject() == null) {
            throw new RuntimeException("Test module not found");
        }
        
        // Check organization boundary
        requireSameOrganization(currentUser, testModule.getProject().getOrganization());
        
        // ADMIN users can access any module
        if (userContextService.isAdmin(currentUser)) {
            return;
        }
        
        // Non-ADMIN users must be assigned to the module
        if (currentUser.getAssignedTestModules() == null || 
            currentUser.getAssignedTestModules().isEmpty()) {
            throw new RuntimeException("You are not assigned to any test modules");
        }
        
        boolean isAssigned = currentUser.getAssignedTestModules().stream()
            .anyMatch(m -> m.getId().equals(testModule.getId()));
        
        if (!isAssigned) {
            throw new RuntimeException("You are not assigned to this test module");
        }
    }

    /**
     * Check if a user is assigned to a specific test module.
     * ADMIN users are always considered assigned.
     * @param currentUser the current user
     * @param testModule the test module to check
     * @return true if user can access the module
     */
    public boolean canAccessModule(User currentUser, TestModule testModule) {
        if (currentUser == null || testModule == null) {
            return false;
        }
        
        // Check organization boundary
        if (testModule.getProject() == null || 
            !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            return false;
        }
        
        // ADMIN users can access any module
        if (userContextService.isAdmin(currentUser)) {
            return true;
        }
        
        // Non-ADMIN users must be assigned
        if (currentUser.getAssignedTestModules() == null || 
            currentUser.getAssignedTestModules().isEmpty()) {
            return false;
        }
        
        return currentUser.getAssignedTestModules().stream()
            .anyMatch(m -> m.getId().equals(testModule.getId()));
    }
}
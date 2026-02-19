package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.dto.ModuleAssignmentRequest;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.SubmoduleRepository;
import com.yourproject.tcm.repository.ProjectRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import com.yourproject.tcm.service.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Domain service for TestModule-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ModuleService {

    private final TestModuleRepository testModuleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final SecurityHelper securityHelper;
    private final EntityManager entityManager;
    private final TestCaseService testCaseService;
    private final SubmoduleService submoduleService;

    @Autowired
    public ModuleService(TestModuleRepository testModuleRepository,
                        SubmoduleRepository submoduleRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        UserContextService userContextService,
                        SecurityHelper securityHelper,
                        EntityManager entityManager,
                        TestCaseService testCaseService,
                        SubmoduleService submoduleService) {
        this.testModuleRepository = testModuleRepository;
        this.submoduleRepository = submoduleRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.securityHelper = securityHelper;
        this.entityManager = entityManager;
        this.testCaseService = testCaseService;
        this.submoduleService = submoduleService;
    }

    /**
     * Get all test modules in the organization.
     * For ADMIN users: returns all modules for their organization
     * For QA/BA/TESTER users: returns modules they have ANY access to (Direct or via Project)
     */
    public List<TestModule> getAllModulesInOrganization() {
        User currentUser = userContextService.getCurrentUser();
        if (userContextService.isAdmin(currentUser)) {
            Organization org = currentUser.getOrganization();
            if (org == null) {
                return List.of();
            }
            // Get all projects in organization, then get their modules
            List<Project> orgProjects = projectRepository.findAllByOrganization(org);
            List<TestModule> allModules = new ArrayList<>();
            for (Project project : orgProjects) {
                allModules.addAll(project.getModules());
            }
            return allModules;
        } else if (userContextService.isProjectManager(currentUser)) {
            // PROJECT_MANAGER sees all modules in their assigned projects
            if (currentUser.getAssignedProjects() == null || currentUser.getAssignedProjects().isEmpty()) {
                return List.of();
            }
            List<TestModule> pmModules = new ArrayList<>();
            for (Project project : currentUser.getAssignedProjects()) {
                if (project.getModules() != null) {
                    pmModules.addAll(project.getModules());
                }
            }
            return pmModules;
        } else {
            return testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
        }
    }

    /**
     * Get test modules assigned to the current user.
     * Updated: If user is ADMIN, return all modules in the organization.
     * If user is PROJECT_MANAGER, return all modules in their assigned projects.
     */
    public List<TestModule> getTestModulesAssignedToCurrentUser() {
        User currentUser = userContextService.getCurrentUser();
        
        if (userContextService.isAdmin(currentUser)) {
            Organization org = currentUser.getOrganization();
            if (org == null) {
                return List.of();
            }
            // Get all projects in organization, then get their modules
            List<Project> orgProjects = projectRepository.findAllByOrganization(org);
            List<TestModule> allModules = new ArrayList<>();
            for (Project project : orgProjects) {
                allModules.addAll(project.getModules());
            }
            return allModules;
        }
        // For PROJECT_MANAGER, return all modules in their assigned projects
        if (userContextService.currentUserIsProjectManager()) {
            Set<Project> assignedProjects = currentUser.getAssignedProjects();
            if (assignedProjects == null || assignedProjects.isEmpty()) {
                return List.of();
            }
            List<TestModule> allModules = new ArrayList<>();
            for (Project project : assignedProjects) {
                allModules.addAll(project.getModules());
            }
            return allModules;
        }
        // For QA/BA/TESTER, return modules assigned to them
        return testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
    }

    /**
     * Get all modules in a specific project.
     * Used for module assignment dialog in project team page.
     */
    public List<TestModule> getModulesByProjectId(Long projectId) {
        return testModuleRepository.findByProjectId(projectId);
    }

    /**
     * Get all modules in a specific project with submodules fetched.
     * Used for filter dropdowns that need submodule data.
     */
    public List<TestModule> getModulesByProjectIdWithSubmodules(Long projectId) {
        return testModuleRepository.findByProjectIdWithSubmodules(projectId);
    }

    /**
     * Get a test module by ID with all its submodules and test cases.
     * All users in the same organization can view modules (read-only access).
     * ADMIN users can access any module in their organization (full access).
     * Non-ADMIN users can only edit modules they are assigned to.
     */
    public Optional<TestModule> getTestModuleById(Long testModuleId) {
        User currentUser = userContextService.getCurrentUser();
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        
        if (testModuleOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TestModule testModule = testModuleOpt.get();
        
        // Check organization boundary via project
        Project project = testModule.getProject();
        if (project == null) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        // Check organization boundary - all users in same organization can view
        securityHelper.requireSameOrganization(currentUser, project.getOrganization());
        
        // Fetch all submodules with their test cases for this module
        List<Submodule> submodulesWithTestCases = submoduleRepository.findByTestModuleIdWithTestCases(testModuleId);
        
        // Sort test cases within each submodule by ID
        for (Submodule submodule : submodulesWithTestCases) {
            if (submodule.getTestCases() != null) {
                submodule.getTestCases().sort(Comparator.comparing(TestCase::getId));
            }
        }
        
        // Set the complete submodules list with test cases
        testModule.setSubmodules(submodulesWithTestCases);
        return Optional.of(testModule);
    }

    /**
     * Create a test module for a project.
     * ADMIN users can create modules in any project.
     * PROJECT_MANAGER users can create modules in their assigned projects.
     */
    @Transactional
    public TestModule createTestModuleForProject(Long projectId, TestModule testModule) {
        User currentUser = userContextService.getCurrentUser();
        
        // Allow ADMIN or PROJECT_MANAGER to create modules
        if (!userContextService.isAdmin(currentUser) && 
            !userContextService.isProjectManager(currentUser)) {
            throw new RuntimeException("Only ADMIN or PROJECT_MANAGER users can create modules");
        }
        
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            
            // Check organization boundary
            securityHelper.requireSameOrganization(currentUser, project.getOrganization());
            
            // For PROJECT_MANAGER, check if they're assigned to this project
            if (userContextService.isProjectManager(currentUser)) {
                if (currentUser.getAssignedProjects() == null || 
                    !currentUser.getAssignedProjects().contains(project)) {
                    throw new RuntimeException("You are not assigned to this project");
                }
            }
            
            testModule.setProject(project);
            TestModule savedTestModule = testModuleRepository.save(testModule);
            entityManager.flush();
            return savedTestModule;
        } else {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
    }

    /**
     * Update a test module.
     * ADMIN users can update any module in their organization.
     * PROJECT_MANAGER users can update any module in their assigned projects.
     * QA/BA users can only update modules they are assigned to.
     */
    @Transactional
    public TestModule updateTestModule(Long testModuleId, TestModule testModuleDetails) {
        User currentUser = userContextService.getCurrentUser();
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
        
        TestModule testModule = testModuleOpt.get();
        
        // Check organization boundary via project
        Project project = testModule.getProject();
        if (project == null) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        securityHelper.requireSameOrganization(currentUser, project.getOrganization());
        
        // ADMIN can edit any module
        // PROJECT_MANAGER can edit any module in their assigned projects
        // QA/BA can only edit assigned modules
        if (!userContextService.isAdmin(currentUser)) {
            // For PROJECT_MANAGER, check if they're assigned to this project
            if (userContextService.isProjectManager(currentUser)) {
                if (currentUser.getAssignedProjects() == null || 
                    !currentUser.getAssignedProjects().contains(project)) {
                    throw new RuntimeException("You are not assigned to this project");
                }
            } else {
                // For QA/BA, check module assignment
                if (!securityHelper.canAccessModule(currentUser, testModule)) {
                    throw new RuntimeException("Access denied: You are not assigned to this test module");
                }
            }
        }
        
        testModule.setName(testModuleDetails.getName());
        testModule.setDescription(testModuleDetails.getDescription());
        TestModule updatedTestModule = testModuleRepository.save(testModule);
        entityManager.flush();
        return updatedTestModule;
    }

    /**
     * Delete a test module and all its contents (cascading delete).
     * ADMIN users can delete any module in their organization.
     * PROJECT_MANAGER can delete modules in their assigned projects.
     */
    @Transactional
    public void deleteTestModule(Long testModuleId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Fetch the module with submodules
        TestModule testModule = testModuleRepository.findByIdWithSubmodules(testModuleId)
                .orElseThrow(() -> new RuntimeException("Test Module not found with id: " + testModuleId));
        
        // Check organization boundary via project
        Project project = testModule.getProject();
        if (project == null) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        securityHelper.requireSameOrganization(currentUser, project.getOrganization());
        
        // For PROJECT_MANAGER, check if they're assigned to this project
        if (userContextService.currentUserIsProjectManager()) {
            Set<Project> assignedProjects = currentUser.getAssignedProjects();
            if (assignedProjects == null || assignedProjects.isEmpty()) {
                throw new RuntimeException("You are not assigned to any project. Please contact an administrator.");
            }
            boolean isAssignedToProject = assignedProjects.stream()
                .anyMatch(p -> p.getId().equals(project.getId()));
            if (!isAssignedToProject) {
                throw new RuntimeException("You can only delete modules in projects you're assigned to");
            }
        } else if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only admin and project manager users can delete modules");
        }
        
        // 1. Clear assignments from junction tables using native SQL
        // Delete from module_editor_assignments
        entityManager.createNativeQuery("DELETE FROM module_editor_assignments WHERE test_module_id = :moduleId")
            .setParameter("moduleId", testModuleId)
            .executeUpdate();
        // Delete from execution_assignees
        entityManager.createNativeQuery("DELETE FROM execution_assignees WHERE test_module_id = :moduleId")
            .setParameter("moduleId", testModuleId)
            .executeUpdate();
        entityManager.flush();

        // 2. Clean up submodules deeply
        // We iterate through a copy and call SubmoduleService to properly delete all contents
        if (testModule.getSubmodules() != null) {
            List<Submodule> submodules = new ArrayList<>(testModule.getSubmodules());
            for (Submodule submodule : submodules) {
                submoduleService.deleteSubmodule(submodule.getId());
            }
        }

        entityManager.flush();

        // 3. Now delete the module structure
        testModuleRepository.delete(testModule);
        entityManager.flush();
    }

    // ... existing code ...

    /**
     * Assign a user to a test module.
     * ADMIN users can assign any user in their organization.
     * Non-ADMIN users cannot assign users to modules.
     * Automatically generates executions for the user for all test cases in the module.
     */
    @Transactional
    public User assignUserToTestModule(ModuleAssignmentRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + request.getTestModuleId());
        }
        TestModule testModule = testModuleOpt.get();

        // Check if user has permission to manage this module
        if (!userContextService.isAdmin(currentUser)) {
            securityHelper.requireAdminProjectManagerQaOrBa(currentUser);
            if (!securityHelper.canAccessModule(currentUser, testModule)) {
                throw new RuntimeException("Access denied: You are not assigned to this test module");
            }
        }
        
        Optional<User> userOpt = userRepository.findById(request.getUserId());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check organization boundary for both user and module
            if (!user.getOrganization().getId().equals(currentUser.getOrganization().getId()) ||
                !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("User and test module must belong to the same organization as the assigner");
            }

            // Add module to user's assigned modules for execution if not already assigned
            if (!user.getAssignedModulesForExecution().contains(testModule)) {
                user.getAssignedModulesForExecution().add(testModule);
                User savedUser = userRepository.save(user);
                entityManager.flush();

                // Auto-generate executions for the user for all test cases in this module
                // This ensures they immediately see tasks in their workbench
                try {
                    // We need to fetch submodules with test cases to iterate
                    List<Submodule> submodules = submoduleRepository.findByTestModuleIdWithTestCases(testModule.getId());
                    for (Submodule submodule : submodules) {
                        if (submodule.getTestCases() != null) {
                            for (TestCase testCase : submodule.getTestCases()) {
                                try {
                                    testCaseService.autoGenerateTestExecution(testCase.getId(), user.getId());
                                } catch (Exception e) {
                                    // Ignore if execution already exists or other error, continue with others
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log error but don't fail the assignment
                    System.err.println("Error auto-generating executions: " + e.getMessage());
                }
                
                return savedUser;
            } else {
                return user; // Already assigned
            }
        }
        throw new RuntimeException("User or test module not found with id: " + request.getUserId() + " or " + request.getTestModuleId());
    }

    /**
     * Remove a user from a test module.
     * ADMIN users can remove any user in their organization.
     * Non-ADMIN users cannot remove users from modules.
     */
    @Transactional
    public User removeUserFromTestModule(ModuleAssignmentRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + request.getTestModuleId());
        }
        TestModule testModule = testModuleOpt.get();

        // Check if user has permission to manage this module
        if (!userContextService.isAdmin(currentUser)) {
            securityHelper.requireAdminProjectManagerQaOrBa(currentUser);
            if (!securityHelper.canAccessModule(currentUser, testModule)) {
                throw new RuntimeException("Access denied: You are not assigned to this test module");
            }
        }
        
        Optional<User> userOpt = userRepository.findById(request.getUserId());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Remove module from user's assigned modules for execution
            if (user.getAssignedModulesForExecution().contains(testModule)) {
                user.getAssignedModulesForExecution().remove(testModule);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Not assigned
            }
        }
        throw new RuntimeException("User or test module not found with id: " + request.getUserId() + " or " + request.getTestModuleId());
    }
}
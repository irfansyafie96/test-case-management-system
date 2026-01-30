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
    private final EntityManager entityManager;

    @Autowired
    public ModuleService(TestModuleRepository testModuleRepository,
                        SubmoduleRepository submoduleRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        UserContextService userContextService,
                        EntityManager entityManager) {
        this.testModuleRepository = testModuleRepository;
        this.submoduleRepository = submoduleRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.entityManager = entityManager;
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
        } else {
            return testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
        }
    }

    /**
     * Get test modules assigned to the current user.
     * Updated: If user is ADMIN, return all modules in the organization.
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
        return testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
    }

    /**
     * Get a test module by ID with all its submodules and test cases.
     * ADMIN users can access any module in their organization.
     * Non-ADMIN users can only access modules they are assigned to.
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
        if (project == null || !project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        // ADMIN users can access any module in their organization
        if (userContextService.isAdmin(currentUser)) {
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
        
        // Non-ADMIN users can only access modules they are assigned to
        if (currentUser.getAssignedTestModules().contains(testModule)) {
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
        
        throw new RuntimeException("Access denied: You are not assigned to this test module");
    }

    /**
     * Create a test module for a project.
     * Only ADMIN users can create modules.
     */
    @Transactional
    public TestModule createTestModuleForProject(Long projectId, TestModule testModule) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can create test modules");
        }
        
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            
            // Check organization boundary
            if (!project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Project not found or access denied");
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
     * Non-ADMIN users can only update modules they are assigned to.
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
        if (project == null || !project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        // ADMIN users can update any module in their organization
        // Non-ADMIN users can only update modules they are assigned to
        if (!userContextService.isAdmin(currentUser) && !currentUser.getAssignedTestModules().contains(testModule)) {
            throw new RuntimeException("Access denied: You are not assigned to this test module");
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
     * Non-ADMIN users cannot delete modules (only update).
     */
    @Transactional
    public void deleteTestModule(Long testModuleId) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can delete test modules");
        }
        
        // Fetch the module with submodules
        TestModule testModule = testModuleRepository.findByIdWithSubmodules(testModuleId)
                .orElseThrow(() -> new RuntimeException("Test Module not found with id: " + testModuleId));
        
        // Check organization boundary via project
        Project project = testModule.getProject();
        if (project == null || !project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        // 1. Clear assignments from junction table using native SQL
        // This ensures junction table records are removed before attempting to delete the module
        entityManager.createNativeQuery("DELETE FROM user_test_modules WHERE test_module_id = :moduleId")
            .setParameter("moduleId", testModuleId)
            .executeUpdate();
        entityManager.flush();

        // 2. Clean up submodules deeply
        // We iterate through a copy to perform deep cleanup via SubmoduleService
        if (testModule.getSubmodules() != null) {
            List<Submodule> submodules = new ArrayList<>(testModule.getSubmodules());
            for (Submodule submodule : submodules) {
                // TODO: Call SubmoduleService to delete submodule
                // For now, just remove from collection and let orphanRemoval handle it
                // This is not ideal as it doesn't clean up test cases and executions
                testModule.getSubmodules().remove(submodule);
            }
        }

        entityManager.flush();

        // 3. Now delete the module structure
        testModuleRepository.delete(testModule);
        entityManager.flush();
    }

    /**
     * Assign a user to a test module.
     * ADMIN users can assign any user in their organization.
     * Non-ADMIN users cannot assign users to modules.
     */
    @Transactional
    public User assignUserToTestModule(ModuleAssignmentRequest request) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can assign users to test modules");
        }
        
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());

        if (userOpt.isPresent() && testModuleOpt.isPresent()) {
            User user = userOpt.get();
            TestModule testModule = testModuleOpt.get();
            
            // Check organization boundary for both user and module
            if (!user.getOrganization().getId().equals(currentUser.getOrganization().getId()) ||
                !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("User and test module must belong to the same organization as the assigner");
            }

            // Add module to user's assigned modules if not already assigned
            if (!user.getAssignedTestModules().contains(testModule)) {
                user.getAssignedTestModules().add(testModule);
                User savedUser = userRepository.save(user);
                entityManager.flush();
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
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can remove users from test modules");
        }
        
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());

        if (userOpt.isPresent() && testModuleOpt.isPresent()) {
            User user = userOpt.get();
            TestModule testModule = testModuleOpt.get();

            // Remove module from user's assigned modules
            if (user.getAssignedTestModules().contains(testModule)) {
                user.getAssignedTestModules().remove(testModule);
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
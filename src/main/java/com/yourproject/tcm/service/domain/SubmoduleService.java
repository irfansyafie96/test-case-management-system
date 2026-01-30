package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.TestStep;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.repository.SubmoduleRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.TestExecutionRepository;
import com.yourproject.tcm.repository.TestStepResultRepository;
import com.yourproject.tcm.service.UserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for Submodule-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class SubmoduleService {

    private final SubmoduleRepository submoduleRepository;
    private final TestModuleRepository testModuleRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestStepResultRepository testStepResultRepository;
    private final UserContextService userContextService;
    private final EntityManager entityManager;

    @Autowired
    public SubmoduleService(SubmoduleRepository submoduleRepository,
                           TestModuleRepository testModuleRepository,
                           TestExecutionRepository testExecutionRepository,
                           TestStepResultRepository testStepResultRepository,
                           UserContextService userContextService,
                           EntityManager entityManager) {
        this.submoduleRepository = submoduleRepository;
        this.testModuleRepository = testModuleRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testStepResultRepository = testStepResultRepository;
        this.userContextService = userContextService;
        this.entityManager = entityManager;
    }

    /**
     * Get a submodule by ID with its module information.
     * ADMIN users can access any submodule in their organization.
     * Non-ADMIN users can only access submodules in modules they are assigned to.
     */
    public Optional<Submodule> getSubmoduleById(Long submoduleId) {
        User currentUser = userContextService.getCurrentUser();
        Optional<Submodule> submoduleOpt = submoduleRepository.findByIdWithModule(submoduleId);
        
        if (submoduleOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Submodule submodule = submoduleOpt.get();
        TestModule testModule = submodule.getTestModule();
        
        if (testModule == null) {
            throw new RuntimeException("Submodule not found or access denied");
        }
        
        // Check organization boundary via module -> project
        if (!testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Submodule not found or access denied");
        }
        
        // ADMIN users can access any submodule in their organization
        if (userContextService.isAdmin(currentUser)) {
            return submoduleOpt;
        }
        
        // Non-ADMIN users can only access submodules in modules they are assigned to
        if (currentUser.getAssignedTestModules().contains(testModule)) {
            return submoduleOpt;
        }
        
        throw new RuntimeException("Access denied: You are not assigned to the parent module of this submodule");
    }

    /**
     * Get all submodules for a test module with their test cases.
     * ADMIN users can access any module's submodules in their organization.
     * Non-ADMIN users can only access submodules in modules they are assigned to.
     */
    public List<Submodule> getSubmodulesByModuleId(Long moduleId) {
        User currentUser = userContextService.getCurrentUser();
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(moduleId);
        
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test Module not found with id: " + moduleId);
        }
        
        TestModule testModule = testModuleOpt.get();
        
        // Check organization boundary via module -> project
        if (!testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Test Module not found or access denied");
        }
        
        // ADMIN users can access any module's submodules in their organization
        if (userContextService.isAdmin(currentUser)) {
            return submoduleRepository.findByTestModuleIdWithTestCases(moduleId);
        }
        
        // Non-ADMIN users can only access submodules in modules they are assigned to
        if (currentUser.getAssignedTestModules().contains(testModule)) {
            return submoduleRepository.findByTestModuleIdWithTestCases(moduleId);
        }
        
        throw new RuntimeException("Access denied: You are not assigned to this test module");
    }

    /**
     * Create a submodule for a test module.
     * ADMIN users can create submodules in any module in their organization.
     * Non-ADMIN users cannot create submodules.
     */
    @Transactional
    public Submodule createSubmoduleForTestModule(Long testModuleId, Submodule submodule) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can create submodules");
        }
        
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();
            
            // Check organization boundary via module -> project
            if (!testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Test Module not found or access denied");
            }
            
            submodule.setTestModule(testModule);
            Submodule savedSubmodule = submoduleRepository.save(submodule);
            entityManager.flush();
            return savedSubmodule;
        } else {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
    }

    /**
     * Update a submodule.
     * ADMIN users can update any submodule in their organization.
     * Non-ADMIN users cannot update submodules.
     */
    @Transactional
    public Submodule updateSubmodule(Long submoduleId, Submodule submoduleDetails) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can update submodules");
        }
        
        Optional<Submodule> submoduleOpt = submoduleRepository.findById(submoduleId);
        if (submoduleOpt.isPresent()) {
            Submodule submodule = submoduleOpt.get();
            
            // Check organization boundary via module -> project
            TestModule testModule = submodule.getTestModule();
            if (testModule == null || !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Submodule not found or access denied");
            }
            
            submodule.setName(submoduleDetails.getName());
            Submodule updatedSubmodule = submoduleRepository.save(submodule);
            entityManager.flush();
            return updatedSubmodule;
        } else {
            throw new RuntimeException("Submodule not found with id: " + submoduleId);
        }
    }

    /**
     * Delete a submodule and all its contents (cascading delete).
     * ADMIN users can delete any submodule in their organization.
     * QA/BA users can delete submodules in modules they are assigned to.
     */
    @Transactional
    public void deleteSubmodule(Long submoduleId) {
        User currentUser = userContextService.getCurrentUser();
        
        Optional<Submodule> submoduleOpt = submoduleRepository.findById(submoduleId);
        if (submoduleOpt.isEmpty()) {
            throw new RuntimeException("Submodule not found with id: " + submoduleId);
        }
        
        Submodule submodule = submoduleOpt.get();
        
        // Check organization boundary via module -> project
        TestModule testModule = submodule.getTestModule();
        if (testModule == null || !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Submodule not found or access denied");
        }
        
        // ADMIN users can delete any submodule in their organization
        if (!userContextService.isAdmin(currentUser)) {
            // Non-ADMIN users can only delete submodules in modules they are assigned to
            if (!userContextService.isQaOrBa(currentUser)) {
                throw new RuntimeException("Access denied: Only ADMIN, QA, or BA users can delete submodules");
            }
            
            // Check if user is assigned to the module
            boolean isAssignedToModule = currentUser.getAssignedTestModules().contains(testModule);
            if (!isAssignedToModule) {
                throw new RuntimeException("Access denied: You are not assigned to the parent module of this submodule");
            }
        }
        
        // First, cleanup all execution data for test cases in the submodule
        if (submodule.getTestCases() != null) {
            // We don't delete the test case entity here directly
            // We just clean up the 'grandchildren' (Executions/Results)
            // The test cases themselves will be deleted via orphanRemoval when we clear the list
            for (TestCase testCase : submodule.getTestCases()) {
                // Delete all test executions for this test case
                List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCase.getId());
                if (!executions.isEmpty()) {
                    testExecutionRepository.deleteAll(executions);
                }

                // Delete test step results that might still reference the test steps
                if (testCase.getTestSteps() != null) {
                    for (TestStep step : testCase.getTestSteps()) {
                        testStepResultRepository.deleteByTestStepId(step.getId());
                    }
                }
            }

            // Clear the collection to trigger orphanRemoval
            // This deletes the test cases from DB cleanly without setting FK to null
            submodule.getTestCases().clear();
            entityManager.flush(); // Force the deletion of test cases
        }

        // Now delete the submodule
        submoduleRepository.delete(submodule);
        entityManager.flush();
    }
}
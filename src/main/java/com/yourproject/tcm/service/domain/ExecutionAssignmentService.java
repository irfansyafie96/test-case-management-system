package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.ExecutionAssignee;
import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.ExecutionAssignmentRequest;
import com.yourproject.tcm.repository.ExecutionAssigneeRepository;
import com.yourproject.tcm.repository.SubmoduleRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ExecutionAssignmentService
 * 
 * Service for managing execution assignee assignments.
 * QA, BA, and TESTER users can be assigned as execution assignees.
 * 
 * When a user is assigned as an execution assignee, test executions are
 * automatically generated for all test cases in the module, with the assigned
 * user set as the executor.
 */
@Service
@Transactional
public class ExecutionAssignmentService {

    private final ExecutionAssigneeRepository executionAssigneeRepository;
    private final UserRepository userRepository;
    private final TestModuleRepository testModuleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final TestCaseService testCaseService;
    private final UserContextService userContextService;
    private final EntityManager entityManager;

    @Autowired
    public ExecutionAssignmentService(
            ExecutionAssigneeRepository executionAssigneeRepository,
            UserRepository userRepository,
            TestModuleRepository testModuleRepository,
            SubmoduleRepository submoduleRepository,
            TestCaseService testCaseService,
            UserContextService userContextService,
            EntityManager entityManager) {
        this.executionAssigneeRepository = executionAssigneeRepository;
        this.userRepository = userRepository;
        this.testModuleRepository = testModuleRepository;
        this.submoduleRepository = submoduleRepository;
        this.testCaseService = testCaseService;
        this.userContextService = userContextService;
        this.entityManager = entityManager;
    }

    /**
     * Assign a user as an execution assignee.
     * QA, BA, and TESTER users can be assigned as execution assignees.
     * 
     * Automatically generates test executions for all test cases in the module.
     * 
     * @param request The assignment request containing userId and testModuleId
     * @return The updated user with their assigned modules
     * @throws RuntimeException if user not found or module not found
     */
    public User assignExecutor(ExecutionAssignmentRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        // Fetch test module
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + request.getTestModuleId());
        }
        TestModule testModule = testModuleOpt.get();

        // Fetch user to assign
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + request.getUserId());
        }
        User user = userOpt.get();

        // Check organization boundary
        if (!user.getOrganization().getId().equals(currentUser.getOrganization().getId()) ||
            !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("User and test module must belong to the same organization as the assigner");
        }

        // Check if assignment already exists
        if (executionAssigneeRepository.existsByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId())) {
            throw new RuntimeException("User is already assigned as an execution assignee for this module");
        }

        // Create assignment
        ExecutionAssignee assignment = new ExecutionAssignee(user, testModule, currentUser.getUsername());
        executionAssigneeRepository.save(assignment);
        entityManager.flush();

        // Auto-generate executions for the user for all test cases in this module
        autoGenerateExecutions(user, testModule);

        // Refresh user to get updated assignments
        User refreshedUser = userRepository.findById(user.getId()).orElse(user);
        return refreshedUser;
    }

    /**
     * Remove a user as an execution assignee.
     * 
     * @param request The removal request containing userId and testModuleId
     * @return The updated user
     * @throws RuntimeException if assignment not found
     */
    public User removeExecutor(ExecutionAssignmentRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        // Check if assignment exists
        if (!executionAssigneeRepository.existsByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId())) {
            throw new RuntimeException("User is not assigned as an execution assignee for this module");
        }

        // Delete assignment
        executionAssigneeRepository.deleteByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId());
        entityManager.flush();

        // Refresh user to get updated assignments
        User refreshedUser = userRepository.findById(request.getUserId()).orElse(null);
        return refreshedUser;
    }

    /**
     * Get all execution assignees for a specific test module.
     * 
     * @param testModuleId The test module ID
     * @return List of users assigned as execution assignees
     */
    public List<User> getExecutionAssignees(Long testModuleId) {
        return executionAssigneeRepository.findAssigneesByModuleIdWithRoles(testModuleId);
    }

    /**
     * Auto-generate test executions for a user for all test cases in a module.
     * This ensures the user immediately sees tasks in their workbench.
     * 
     * @param user The user to generate executions for
     * @param testModule The test module containing the test cases
     */
    private void autoGenerateExecutions(User user, TestModule testModule) {
        try {
            // Fetch submodules with test cases
            List<Submodule> submodules = submoduleRepository.findByTestModuleIdWithTestCases(testModule.getId());
            
            for (Submodule submodule : submodules) {
                if (submodule.getTestCases() != null) {
                    for (TestCase testCase : submodule.getTestCases()) {
                        try {
                            // Use autoGenerateTestExecution to bypass ADMIN check
                            testCaseService.autoGenerateTestExecution(testCase.getId(), user.getId());
                        } catch (Exception e) {
                            // Ignore if execution already exists or other error, continue with others
                            System.err.println("Error auto-generating execution for test case " + 
                                    testCase.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the assignment
            System.err.println("Error auto-generating executions for module " + 
                    testModule.getId() + ": " + e.getMessage());
        }
    }
}
package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.StepResultResponse;
import com.yourproject.tcm.model.dto.TestExecutionDTO;
import com.yourproject.tcm.repository.*;
import com.yourproject.tcm.service.SecurityHelper;
import com.yourproject.tcm.service.UserContextService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service for TestExecution-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ExecutionService {

    private final TestExecutionRepository testExecutionRepository;
    private final UserRepository userRepository;
    private final TestStepResultRepository testStepResultRepository;
    private final UserContextService userContextService;
    private final SecurityHelper securityHelper;
    private final TestModuleRepository testModuleRepository;
    private final SubmoduleRepository submoduleRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ExecutionService(TestExecutionRepository testExecutionRepository, 
                           UserRepository userRepository,
                           TestStepResultRepository testStepResultRepository,
                           UserContextService userContextService,
                           SecurityHelper securityHelper,
                           TestModuleRepository testModuleRepository,
                           SubmoduleRepository submoduleRepository,
                           TestCaseRepository testCaseRepository,
                           TestCaseService testCaseService) {
        this.testExecutionRepository = testExecutionRepository;
        this.userRepository = userRepository;
        this.testStepResultRepository = testStepResultRepository;
        this.userContextService = userContextService;
        this.securityHelper = securityHelper;
        this.testModuleRepository = testModuleRepository;
        this.submoduleRepository = submoduleRepository;
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
    }

    /**
     * Get all executions in the organization.
     * Used for admin filtering on execution page - returns all executions (not just latest per test case)
     * This allows admins to filter by assigned user and see all executions assigned to that user
     * @param userId Optional user ID to filter by - when provided, only shows executions from modules the user is currently assigned to
     * @return List of all executions in the organization as DTOs
     */
    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getAllExecutionsInOrganization(Long userId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Only admin users can access this
        securityHelper.requireAdmin(currentUser);

        Organization org = currentUser.getOrganization();
        if (org == null) {
            throw new RuntimeException("User does not belong to any organization");
        }

        // Get all executions in the organization (not just latest per test case)
        Long orgId = org.getId();
        List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);

        // If userId is provided, filter by that user's assigned executions
        if (userId != null) {
            allExecutions = allExecutions.stream()
                .filter(execution -> execution.getAssignedToUser() != null && 
                                     execution.getAssignedToUser().getId().equals(userId))
                .collect(Collectors.toList());
        }

        // Return filtered executions as DTOs with hierarchical sorting
        return allExecutions.stream()
            .sorted((e1, e2) -> {
                // Compare by Module ID
                int moduleCompare = Long.compare(e1.getModuleId(), e2.getModuleId());
                if (moduleCompare != 0) return moduleCompare;

                // Compare by Submodule ID
                int suiteCompare = Long.compare(e1.getSubmoduleId(), e2.getSubmoduleId());
                if (suiteCompare != 0) return suiteCompare;

                // Compare by Test Case ID
                Long tcId1 = e1.getTestCase() != null ? e1.getTestCase().getId() : null;
                Long tcId2 = e2.getTestCase() != null ? e2.getTestCase().getId() : null;
                if (tcId1 != null && tcId2 != null) {
                    return Long.compare(tcId1, tcId2);
                }
                return 0;
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get a test execution by ID with security checks.
     * Validates organization boundary and user assignments.
     */
    @Transactional(readOnly = true)
    public Optional<TestExecution> getTestExecutionById(Long executionId) {
        User currentUser = userContextService.getCurrentUser();
        
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        if (executionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TestExecution execution = executionOpt.get();
        
        // Check organization boundary via test case → submodule → module → project → organization
        if (execution.getTestCase() == null || 
            !execution.getTestCase().getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            return Optional.empty();
        }
        
        // ADMIN users can access any execution in their organization
        if (userContextService.isAdmin(currentUser)) {
            return executionOpt;
        }
        
        // Non-ADMIN users can only access executions:
        // 1. Assigned to them, OR
        // 2. In projects/modules they are assigned to
        Project project = execution.getTestCase().getSubmodule().getTestModule().getProject();
        boolean isAssignedToProject = currentUser.getAssignedProjects().contains(project);
        boolean isAssignedToModule = currentUser.getAssignedTestModules()
            .contains(execution.getTestCase().getSubmodule().getTestModule());
        boolean isAssignedToExecution = execution.getAssignedToUser() != null && 
            execution.getAssignedToUser().getId().equals(currentUser.getId());
        
        if (isAssignedToExecution || isAssignedToProject || isAssignedToModule) {
            return executionOpt;
        }
        
        return Optional.empty();
    }

    /**
     * Get test executions assigned to a user with security checks.
     * Only ADMIN users can view executions assigned to other users.
     * Non-ADMIN users can only view their own assigned executions.
     */
    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getTestExecutionsAssignedToUser(Long userId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Check if user is trying to view another user's executions
        if (!currentUser.getId().equals(userId) && !userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Access denied: You can only view your own assigned executions");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        User targetUser = userOpt.get();
        
        // Verify organization boundary
        securityHelper.requireSameOrganization(currentUser, targetUser.getOrganization());
        
        List<TestExecution> executions = testExecutionRepository.findByAssignedToUserWithDetails(targetUser);
        
        // Filter executions to ensure they belong to the same organization (extra safety)
        return executions.stream()
            .filter(execution -> {
                if (execution.getTestCase() == null) return false;
                Long executionOrgId = execution.getTestCase().getSubmodule().getTestModule().getProject()
                    .getOrganization().getId();
                return executionOrgId.equals(currentUser.getOrganization().getId());
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get test executions for the current user with security checks.
     * - ADMIN users see all executions in their organization, but only latest per test case
     * - Non-ADMIN users see only executions assigned to them for modules they're assigned to
     */
    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getTestExecutionsForCurrentUser() {
        User currentUser = userContextService.getCurrentUser();
        
        // If user is ADMIN, return all executions but only one per test case (latest)
        if (userContextService.isAdmin(currentUser)) {
            // Ensure we only get executions for the user's organization
            Long orgId = currentUser.getOrganization() != null ? currentUser.getOrganization().getId() : -1L;
            List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);
            Map<Long, TestExecution> latestExecutionByTestCase = new HashMap<>();

            for (TestExecution execution : allExecutions) {
                Long testCaseId = execution.getTestCase().getId();
                // Keep only the latest execution for each test case
                if (!latestExecutionByTestCase.containsKey(testCaseId) ||
                    execution.getExecutionDate().isAfter(latestExecutionByTestCase.get(testCaseId).getExecutionDate())) {
                    latestExecutionByTestCase.put(testCaseId, execution);
                }
            }
            return latestExecutionByTestCase.values().stream()
                // Add hierarchical sorting: Module ID → Suite ID → Test Case ID
                .sorted((e1, e2) -> {
                    // Compare by Module ID
                    int moduleCompare = Long.compare(e1.getModuleId(), e2.getModuleId());
                    if (moduleCompare != 0) return moduleCompare;

                    // Compare by Submodule ID
                    int suiteCompare = Long.compare(e1.getSubmoduleId(), e2.getSubmoduleId());
                    if (suiteCompare != 0) return suiteCompare;

                    // Compare by Test Case ID (use testCase's ID)
                    Long tcId1 = e1.getTestCase().getId();
                    Long tcId2 = e2.getTestCase().getId();
                    return Long.compare(tcId1, tcId2);
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }

        // Otherwise return only executions assigned to the user for modules they're currently assigned to
        List<TestExecution> allAssignedExecutions = testExecutionRepository.findByAssignedToUserWithDetails(currentUser);
        Set<Long> assignedModuleIds = currentUser.getAssignedTestModules().stream()
            .map(TestModule::getId)
            .collect(Collectors.toSet());

        return allAssignedExecutions.stream()
            .filter(execution -> {
                Long moduleId = execution.getModuleId();
                return moduleId != null && assignedModuleIds.contains(moduleId);
            })
            // Add hierarchical sorting: Module ID → Suite ID → Test Case ID
            .sorted((e1, e2) -> {
                // Compare by Module ID
                int moduleCompare = Long.compare(e1.getModuleId(), e2.getModuleId());
                if (moduleCompare != 0) return moduleCompare;

                // Compare by Submodule ID
                int suiteCompare = Long.compare(e1.getSubmoduleId(), e2.getSubmoduleId());
                if (suiteCompare != 0) return suiteCompare;

                // Compare by Test Case ID (use numeric ID comparison for consistency)
                Long tcId1 = e1.getTestCase() != null ? e1.getTestCase().getId() : null;
                Long tcId2 = e2.getTestCase() != null ? e2.getTestCase().getId() : null;
                if (tcId1 != null && tcId2 != null) {
                    return Long.compare(tcId1, tcId2);
                }
                return 0;
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Complete a test execution with the overall result and optional notes.
     * Only the assigned user or ADMIN can complete an execution.
     * Redmine issue fields can be provided when the test case fails.
     */
    @Transactional
    public TestExecution completeTestExecution(Long executionId, String overallResult, String notes,
                                                String bugReportSubject, String bugReportDescription, String redmineIssueUrl) {
        User currentUser = userContextService.getCurrentUser();
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        if (executionOpt.isEmpty()) {
            throw new RuntimeException("Test Execution not found with id: " + executionId);
        }
        
        TestExecution execution = executionOpt.get();
        
        // Check organization boundary via test case → submodule → module → project → organization
        securityHelper.requireSameOrganization(currentUser, execution.getTestCase().getSubmodule().getTestModule().getProject().getOrganization());
        
        // ADMIN users can complete any execution in their organization
        if (!userContextService.isAdmin(currentUser)) {
            // Non-ADMIN users can only complete executions assigned to them
            if (execution.getAssignedToUser() == null || 
                !execution.getAssignedToUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only complete executions assigned to you");
            }
        }
        
        execution.setOverallResult(overallResult);
        execution.setNotes(notes);
        execution.setCompletionDate(LocalDateTime.now());
        execution.setStatus("COMPLETED");
        
        // Save Redmine integration data if provided and execution failed
        if ("FAILED".equals(overallResult)) {
            if (bugReportSubject != null) {
                execution.setBugReportSubject(bugReportSubject);
            }
            if (bugReportDescription != null) {
                execution.setBugReportDescription(bugReportDescription);
            }
            if (redmineIssueUrl != null) {
                execution.setRedmineIssueUrl(redmineIssueUrl);
                execution.setRedmineIssueCreatedAt(LocalDateTime.now());
            }
        }
        
        TestExecution savedExecution = testExecutionRepository.save(execution);
        entityManager.flush();
        return savedExecution;
    }

    /**
     * Save execution work-in-progress (notes) without completing the execution.
     * Only the assigned user or ADMIN can save work.
     */
    @Transactional
    public TestExecution saveExecutionWork(Long executionId, String notes) {
        User currentUser = userContextService.getCurrentUser();
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        if (executionOpt.isEmpty()) {
            throw new RuntimeException("Test Execution not found with id: " + executionId);
        }

        TestExecution execution = executionOpt.get();

        // Check organization boundary via test case → submodule → module → project → organization
        if (execution.getTestCase() == null ||
            execution.getTestCase().getSubmodule() == null ||
            execution.getTestCase().getSubmodule().getTestModule() == null ||
            execution.getTestCase().getSubmodule().getTestModule().getProject() == null ||
            !execution.getTestCase().getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Access denied: Execution not in your organization");
        }

        // ADMIN users can save work for any execution in their organization
        if (!userContextService.isAdmin(currentUser)) {
            // Non-ADMIN users can save work if:
            // 1. They are the assigned user for this execution
            boolean isAssignedUser = execution.getAssignedToUser() != null &&
                                     execution.getAssignedToUser().getId().equals(currentUser.getId());

            // 2. OR they are assigned to the module this execution belongs to (shared responsibility)
            boolean isAssignedToModule = false;
            if (!isAssignedUser && execution.getTestCase() != null && execution.getTestCase().getSubmodule() != null
                && execution.getTestCase().getSubmodule().getTestModule() != null) {
                Long moduleId = execution.getTestCase().getSubmodule().getTestModule().getId();
                // Direct DB check for robustness
                List<TestModule> assignedModules = testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
                isAssignedToModule = assignedModules.stream().anyMatch(m -> m.getId().equals(moduleId));
            }

            if (!isAssignedUser && !isAssignedToModule) {
                throw new RuntimeException("Access denied: You are not assigned to this execution or its module");
            }
        }

        execution.setNotes(notes);  // Only update notes, don't change overallResult
        TestExecution savedExecution = testExecutionRepository.save(execution);
        entityManager.flush();
        return savedExecution;
    }

    /**
     * Assign a test execution to a user with security checks.
     * Only ADMIN users can assign executions.
     * The target user must have QA, BA, or TESTER role and belong to same organization.
     */
    @Transactional
    public TestExecution assignTestExecutionToUser(Long executionId, Long userId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Only ADMIN users can assign executions
        securityHelper.requireAdmin(currentUser);
        
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (executionOpt.isEmpty() || userOpt.isEmpty()) {
            throw new RuntimeException("Test execution or user not found with id: " + executionId + " or " + userId);
        }

        TestExecution execution = executionOpt.get();
        User user = userOpt.get();

        // Check organization boundary for execution
        securityHelper.requireSameOrganization(currentUser, execution.getTestCase().getSubmodule().getTestModule().getProject().getOrganization());
        
        // Check that target user belongs to same organization
        securityHelper.requireSameOrganization(currentUser, user.getOrganization());

        // Check if user has QA, BA, or TESTER role
        boolean hasValidRole = user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("QA") || role.getName().equals("BA") || role.getName().equals("TESTER"));
        if (!hasValidRole) {
            throw new RuntimeException("User must have QA, BA, or TESTER role to be assigned to test executions");
        }

        execution.setAssignedToUser(user);
        execution.setStatus("IN_PROGRESS");
        if (execution.getStartDate() == null) {
            execution.setStartDate(LocalDateTime.now());
        }
        TestExecution savedExecution = testExecutionRepository.save(execution);
        entityManager.flush();
        return savedExecution;
    }

    /**
     * Update a step result with security checks.
     * Only the assigned user or ADMIN can update step results.
     */
    @Transactional
    public StepResultResponse updateStepResult(Long executionId, Long stepId, String status, String actualResult) {
        User currentUser = userContextService.getCurrentUser();
        
        // First get the execution to check permissions
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        if (executionOpt.isEmpty()) {
            throw new RuntimeException("Test Execution not found with id: " + executionId);
        }
        
        TestExecution execution = executionOpt.get();
        
        // Check organization boundary via test case → submodule → module → project → organization
        if (execution.getTestCase() == null ||
            execution.getTestCase().getSubmodule() == null ||
            execution.getTestCase().getSubmodule().getTestModule() == null ||
            execution.getTestCase().getSubmodule().getTestModule().getProject() == null ||
            !execution.getTestCase().getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Access denied: Execution not in your organization");
        }

        // ADMIN users can update any step result in their organization
        if (!userContextService.isAdmin(currentUser)) {
            // Non-ADMIN users can update step results if:
            // 1. They are the assigned user for this execution
            boolean isAssignedUser = execution.getAssignedToUser() != null &&
                                     execution.getAssignedToUser().getId().equals(currentUser.getId());

            // 2. OR they are assigned to the module this execution belongs to (shared responsibility)
            boolean isAssignedToModule = false;
            if (!isAssignedUser && execution.getTestCase() != null && execution.getTestCase().getSubmodule() != null
                && execution.getTestCase().getSubmodule().getTestModule() != null) {
                Long moduleId = execution.getTestCase().getSubmodule().getTestModule().getId();
                // Direct DB check for robustness
                List<TestModule> assignedModules = testModuleRepository.findTestModulesAssignedToUser(currentUser.getId());
                isAssignedToModule = assignedModules.stream().anyMatch(m -> m.getId().equals(moduleId));
            }

            if (!isAssignedUser && !isAssignedToModule) {
                throw new RuntimeException("Access denied: You are not assigned to this execution or its module");
            }
        }
        
        // Find the step result using repository method (more efficient than looping)
        TestStepResult existingResult = testStepResultRepository.findByTestExecution_IdAndTestStep_Id(executionId, stepId);
        if (existingResult == null) {
            throw new RuntimeException("Step result not found for execution " + executionId + " and step " + stepId);
        }
        
        existingResult.setStatus(status);
        existingResult.setActualResult(actualResult);
        TestStepResult savedResult = testStepResultRepository.save(existingResult);
        entityManager.flush();

        // Return DTO to avoid serialization issues (circular references)
        return new StepResultResponse(
            savedResult.getId(),
            savedResult.getTestStep().getId(),
            savedResult.getStepNumber(),
            savedResult.getActualResult(),
            savedResult.getStatus(),
            savedResult.getTestStep().getAction(),
            savedResult.getTestStep().getExpectedResult()
        );
    }

    /**
     * Regenerate test executions for a module.
     * Creates executions for all assigned users for all test cases in the module.
     * Only ADMIN, QA, or BA users can regenerate executions.
     */
    @Transactional
    public void regenerateExecutionsForModule(Long moduleId) {
        User currentUser = userContextService.getCurrentUser();
        // Check role permissions: only ADMIN, QA, or BA can regenerate executions
        securityHelper.requireAdminQaOrBa(currentUser);
        
        Optional<TestModule> moduleOpt = testModuleRepository.findById(moduleId);
        if (moduleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + moduleId);
        }

        TestModule module = moduleOpt.get();
        Set<User> assignedUsers = module.getAssignedUsers();

        if (assignedUsers != null && !assignedUsers.isEmpty()) {
            for (User user : assignedUsers) {
                createTestExecutionsForModuleAndUser(module, user);
            }
        }
    }

    /**
     * Create test executions for a module and user.
     * Creates executions for all test cases in the module that don't already have an execution for this user.
     */
    private void createTestExecutionsForModuleAndUser(TestModule module, User user) {
        // Fetch all submodules with their test cases for this module in a single query
        List<Submodule> submodules = submoduleRepository.findByTestModuleIdWithTestCases(module.getId());
        if (submodules == null || submodules.isEmpty()) {
            return;
        }

        // Get existing executions for this user to avoid duplicates
        List<TestExecution> existingExecutions = testExecutionRepository.findByAssignedToUser(user);

        // Iterate through all submodules and their test cases
        for (Submodule submodule : submodules) {
            List<TestCase> testCases = submodule.getTestCases();
            if (testCases == null || testCases.isEmpty()) {
                continue;
            }

            // Create execution for each test case if not already exists
            for (TestCase testCase : testCases) {
                // Check if execution already exists for this test case and user
                boolean alreadyExists = existingExecutions.stream()
                    .anyMatch(e -> e.getTestCase() != null && e.getTestCase().getId().equals(testCase.getId()));

                if (!alreadyExists) {
                    try {
                        testCaseService.createTestExecutionForTestCaseAndUser(testCase.getId(), user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other test cases
                    }
                }
            }
        }
    }

    /**
     * Convert TestExecution to DTO with null safety.
     */
    private TestExecutionDTO convertToDTO(TestExecution execution) {
        if (execution.getTestCase() == null) {
            return null;
        }
        
        var testCase = execution.getTestCase();
        var testModule = testCase.getTestModule();
        
        // Handle null testModule
        Long moduleId = null;
        String moduleName = null;
        Long projectId = null;
        String projectName = null;
        
        if (testModule != null) {
            moduleId = testModule.getId();
            moduleName = testModule.getName();
            
            var project = testModule.getProject();
            if (project != null) {
                projectId = project.getId();
                projectName = project.getName();
            }
        }
        
        List<TestExecutionDTO.TestStepResultDTO> stepResultDTOs = null;
        if (execution.getStepResults() != null) {
            stepResultDTOs = execution.getStepResults().stream()
                .map(sr -> new TestExecutionDTO.TestStepResultDTO(
                    sr.getId(),
                    sr.getTestStep() != null ? sr.getTestStep().getId() : null,
                    sr.getStepNumber(),
                    sr.getStatus(),
                    sr.getActualResult(),
                    sr.getTestStep() != null ? sr.getTestStep().getAction() : null,
                    sr.getTestStep() != null ? sr.getTestStep().getExpectedResult() : null
                ))
                .collect(Collectors.toList());
        }
        
        return new TestExecutionDTO(
            execution.getId(),
            execution.getTestCaseId(),
            testCase.getTitle(),
            execution.getExecutionDate(),
            execution.getOverallResult(),
            execution.getNotes(),
            execution.getDuration(),
            execution.getEnvironment(),
            execution.getExecutedBy(),
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getId() : null,
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getUsername() : "",
            testCase.getSubmoduleId(),
            testCase.getSubmoduleName(),
            moduleId,
            moduleName,
            projectId,
            projectName,
            stepResultDTOs
        );
    }
}
package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.TestStep;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.TestStepResult;
import com.yourproject.tcm.model.dto.TestExecutionDTO;
import com.yourproject.tcm.repository.TestCaseRepository;
import com.yourproject.tcm.repository.SubmoduleRepository;
import com.yourproject.tcm.repository.TestExecutionRepository;
import com.yourproject.tcm.repository.TestStepResultRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Domain service for TestCase-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final SubmoduleRepository submoduleRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestStepResultRepository testStepResultRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TestCaseService(TestCaseRepository testCaseRepository, 
                          SubmoduleRepository submoduleRepository,
                          TestExecutionRepository testExecutionRepository,
                          TestStepResultRepository testStepResultRepository,
                          UserRepository userRepository,
                          UserContextService userContextService) {
        this.testCaseRepository = testCaseRepository;
        this.submoduleRepository = submoduleRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testStepResultRepository = testStepResultRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
    }

    /**
     * Get all test cases.
     * Returns test cases filtered by user's organization.
     */
    public List<TestCase> getAllTestCases() {
        User currentUser = userContextService.getCurrentUser();
        Organization org = currentUser.getOrganization();
        if (org == null) {
            return List.of();
        }
        return testCaseRepository.findAllWithDetailsByOrganizationId(org.getId());
    }

    /**
     * Get a test case by ID with security checks.
     * Validates organization boundary and user assignments.
     */
    public Optional<TestCase> getTestCaseById(Long testCaseId) {
        User currentUser = userContextService.getCurrentUser();
        TestCase testCase = testCaseRepository.findByIdWithSteps(testCaseId);
        
        if (testCase == null) {
            return Optional.empty();
        }
        
        // Check organization boundary via submodule → module → project → organization
        if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Test case not found or access denied");
        }
        
        // ADMIN users can access any test case in their organization
        if (userContextService.isAdmin(currentUser)) {
            return Optional.of(testCase);
        }
        
        // Non-ADMIN users can only access test cases in projects/modules they are assigned to
        Project project = testCase.getSubmodule().getTestModule().getProject();
        if (currentUser.getAssignedProjects().contains(project)) {
            return Optional.of(testCase);
        }
        
        // Check module assignment
        if (currentUser.getAssignedTestModules().contains(testCase.getSubmodule().getTestModule())) {
            return Optional.of(testCase);
        }
        
        throw new RuntimeException("Access denied: You are not assigned to this project or module");
    }

    /**
     * Create a test case for a submodule with security checks.
     * Only ADMIN users can create test cases.
     * Auto-generates executions for all users assigned to the module.
     */
    @Transactional
    public TestCase createTestCaseForSubmodule(Long submoduleId, TestCase testCase) {
        User currentUser = userContextService.getCurrentUser();
        
        // Only ADMIN users can create test cases
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Access denied: Only ADMIN users can create test cases");
        }
        
        Optional<Submodule> submoduleOpt = submoduleRepository.findById(submoduleId);
        if (submoduleOpt.isPresent()) {
            Submodule submodule = submoduleOpt.get();
            
            // Verify organization boundary
            if (!submodule.getTestModule().getProject().getOrganization()
                    .getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Access denied: Submodule not in your organization");
            }
            
            testCase.setSubmodule(submodule);
            
            // Set up test steps with sequential step numbers
            if (testCase.getTestSteps() != null) {
                int stepNum = 1;
                for (TestStep step : testCase.getTestSteps()) {
                    step.setTestCase(testCase);
                    step.setStepNumber(stepNum++);
                }
            }
            
            TestCase savedTestCase = testCaseRepository.save(testCase);
            entityManager.flush(); // Ensure data is written to DB
            
            // Auto-generate executions for all users assigned to the module
            TestModule module = submodule.getTestModule();
            if (module != null && module.getAssignedUsers() != null && !module.getAssignedUsers().isEmpty()) {
                for (User user : module.getAssignedUsers()) {
                    try {
                        createTestExecutionForTestCaseAndUser(savedTestCase.getId(), user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other users
                        // In production, consider using a logger
                    }
                }
            }
            
            return savedTestCase;
        } else {
            throw new RuntimeException("Submodule not found with id: " + submoduleId);
        }
    }

    /**
     * Update a test case with security checks.
     * Only ADMIN users can update test cases.
     * Properly handles test steps and cleans up related test step results.
     */
    @Transactional
    public TestCase updateTestCase(Long testCaseId, TestCase testCaseDetails) {
        User currentUser = userContextService.getCurrentUser();
        
        // Only ADMIN users can update test cases
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Access denied: Only ADMIN users can update test cases");
        }
        
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            
            // Verify organization boundary
            if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                    .getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Access denied: Test case not in your organization");
            }
            
            // Update basic properties
            testCase.setTitle(testCaseDetails.getTitle());
            testCase.setTestCaseId(testCaseDetails.getTestCaseId());
            testCase.setDescription(testCaseDetails.getDescription());
            testCase.setPrerequisites(testCaseDetails.getPrerequisites());
            testCase.setExpectedResult(testCaseDetails.getExpectedResult());
            testCase.setTags(testCaseDetails.getTags());
            
            // Handle test steps - properly manage the relationship to avoid cascade issues
            if (testCaseDetails.getTestSteps() != null) {
                // Get or create the current test steps list to maintain the same collection instance
                java.util.List<TestStep> currentSteps = testCase.getTestSteps();
                if (currentSteps == null) {
                    currentSteps = new java.util.ArrayList<>();
                    testCase.setTestSteps(currentSteps);
                }
                
                // Delete related test step results for existing steps to avoid foreign key constraint violations
                for (TestStep existingStep : currentSteps) {
                    testStepResultRepository.deleteByTestStepId(existingStep.getId());
                }
                
                // Clear the existing steps - this should properly handle the cascade
                currentSteps.clear();
                
                // Add new test steps to the same collection instance
                for (int i = 0; i < testCaseDetails.getTestSteps().size(); i++) {
                    TestStep stepDetail = testCaseDetails.getTestSteps().get(i);
                    TestStep newStep = new TestStep();
                    newStep.setStepNumber(i + 1); // Ensure step numbers are sequential (1, 2, 3, etc.)
                    newStep.setAction(stepDetail.getAction());
                    newStep.setExpectedResult(stepDetail.getExpectedResult());
                    newStep.setTestCase(testCase); // Set the back-reference to testCase
                    currentSteps.add(newStep);
                }
            } else {
                // If new test steps are null, clear existing ones
                java.util.List<TestStep> currentSteps = testCase.getTestSteps();
                if (currentSteps != null) {
                    // Delete related test step results first to avoid foreign key constraint violations
                    for (TestStep existingStep : currentSteps) {
                        testStepResultRepository.deleteByTestStepId(existingStep.getId());
                    }
                    
                    // Clear the existing test steps
                    currentSteps.clear();
                }
            }
            
            return testCaseRepository.save(testCase);
        } else {
            throw new RuntimeException("Test case not found with id: " + testCaseId);
        }
    }

    /**
     * Delete a test case with security checks and comprehensive cleanup.
     * ADMIN users can delete any test case in their organization.
     * QA/BA users can delete test cases in modules they are assigned to.
     * Deletes test executions and related test step results before deleting the test case.
     */
    @Transactional
    public void deleteTestCase(Long testCaseId) {
        User currentUser = userContextService.getCurrentUser();
        
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isEmpty()) {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
        
        TestCase testCase = testCaseOpt.get();
        
        // Verify organization boundary
        if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Access denied: Test case not in your organization");
        }
        
        // ADMIN users can delete any test case in their organization
        if (!userContextService.isAdmin(currentUser)) {
            // Non-ADMIN users can only delete test cases in modules they are assigned to
            if (!userContextService.isQaOrBa(currentUser)) {
                throw new RuntimeException("Access denied: Only ADMIN, QA, or BA users can delete test cases");
            }
            
            // Check if user is assigned to the module
            boolean isAssignedToModule = currentUser.getAssignedTestModules().contains(testCase.getSubmodule().getTestModule());
            if (!isAssignedToModule) {
                throw new RuntimeException("Access denied: You are not assigned to the parent module of this test case");
            }
        }
        
        // First, delete all test executions for this test case
        // This will cascade to delete test step results associated with those executions
        java.util.List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCaseId);
        for (TestExecution execution : executions) {
            testExecutionRepository.deleteById(execution.getId());
        }
        
        // Next, delete test step results that might still reference the test steps
        // (in case any are orphaned)
        if (testCase.getTestSteps() != null) {
            for (TestStep step : testCase.getTestSteps()) {
                testStepResultRepository.deleteByTestStepId(step.getId());
            }
        }
        
        // Now delete the test case (cascading should handle test steps)
        testCaseRepository.deleteById(testCaseId);
        entityManager.flush(); // Ensure data is written to DB
    }

    /**
     * Get test executions for a test case with security checks.
     * Validates organization boundary and user assignments.
     */
    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getTestExecutionsByTestCaseId(Long testCaseId) {
        User currentUser = userContextService.getCurrentUser();
        
        // First, get the test case to verify access
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isEmpty()) {
            return List.of();
        }
        
        TestCase testCase = testCaseOpt.get();
        
        // Check organization boundary via submodule → module → project → organization
        if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                .getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Access denied: Test case not in your organization");
        }
        
        // ADMIN users can access any test case in their organization
        if (userContextService.isAdmin(currentUser)) {
            List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCaseId);
            return executions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
        
        // Non-ADMIN users can only access test cases in projects/modules they are assigned to
        Project project = testCase.getSubmodule().getTestModule().getProject();
        if (currentUser.getAssignedProjects().contains(project) || 
            currentUser.getAssignedTestModules().contains(testCase.getSubmodule().getTestModule())) {
            List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCaseId);
            return executions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
        
        throw new RuntimeException("Access denied: You are not assigned to this project or module");
    }

    /**
     * Create a test execution for a test case with security checks.
     * Only users with TESTER, QA, or ADMIN roles who have access to the test case can create executions.
     * Creates step result records for all steps in the test case.
     */
    @Transactional
    public TestExecution createTestExecutionForTestCase(Long testCaseId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Check role permissions: only TESTER, QA, or ADMIN can create executions
        if (!userContextService.isTester(currentUser) && 
            !userContextService.isQaOrBa(currentUser) && 
            !userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Access denied: Only TESTER, QA, or ADMIN users can create test executions");
        }
        
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            
            // Verify organization boundary
            if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                    .getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Access denied: Test case not in your organization");
            }
            
            // Check user assignments for non-ADMIN users
            if (!userContextService.isAdmin(currentUser)) {
                Project project = testCase.getSubmodule().getTestModule().getProject();
                if (!currentUser.getAssignedProjects().contains(project) && 
                    !currentUser.getAssignedTestModules().contains(testCase.getSubmodule().getTestModule())) {
                    throw new RuntimeException("Access denied: You are not assigned to this project or module");
                }
            }
            
            // Create the main execution record
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);  // Link to the test case
            execution.setExecutionDate(LocalDateTime.now());  // Set current time
            execution.setOverallResult("PENDING");  // Default to pending until executed
            execution.setStatus("PENDING");

            TestExecution initialExecution = testExecutionRepository.save(execution);
            entityManager.flush(); // Ensure execution has an ID

            // Create step results for each step in the test case
            if (testCase.getTestSteps() != null) {
                List<TestStepResult> stepResults = testCase.getTestSteps().stream()
                    .map(step -> {
                        TestStepResult result = new TestStepResult();
                        result.setTestExecution(initialExecution); // Link to the execution
                        result.setTestStep(step);  // Link to the step
                        result.setStepNumber(step.getStepNumber());  // Copy step number
                        result.setStatus("PENDING");  // Default to pending until executed
                        return result;
                    })
                    .collect(Collectors.toList());

                // Assign the step results to the execution and save again
                initialExecution.setStepResults(stepResults);
                TestExecution finalExecution = testExecutionRepository.save(initialExecution);
                entityManager.flush();
                return finalExecution;
            }

            entityManager.flush();
            return initialExecution;
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    /**
     * Create a test execution for a test case and assign to a specific user with security checks.
     * Only ADMIN users can assign executions to other users.
     * Creates step result records for all steps in the test case.
     */
    @Transactional
    public TestExecution createTestExecutionForTestCaseAndUser(Long testCaseId, Long userId) {
        User currentUser = userContextService.getCurrentUser();
        
        // Only ADMIN users can assign executions to other users
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Access denied: Only ADMIN users can assign test executions to other users");
        }
        
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (testCaseOpt.isPresent() && userOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            User targetUser = userOpt.get();
            
            // Verify organization boundary for test case
            if (!testCase.getSubmodule().getTestModule().getProject().getOrganization()
                    .getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Access denied: Test case not in your organization");
            }
            
            // Verify target user is in the same organization
            if (!targetUser.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Access denied: Target user not in your organization");
            }
            
            // Optional: Verify target user is assigned to the module/project
            // This is not required but could be added for validation
            
            // Create new test execution
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);
            execution.setExecutionDate(LocalDateTime.now());
            execution.setOverallResult("PENDING");
            execution.setStatus("PENDING");
            execution.setAssignedToUser(targetUser);

            // Create step results for each step in the test case
            List<TestStep> steps = testCase.getTestSteps();
            if (steps != null && !steps.isEmpty()) {
                List<TestStepResult> stepResults = new ArrayList<>();
                for (TestStep step : steps) {
                    TestStepResult stepResult = new TestStepResult();
                    stepResult.setTestExecution(execution);
                    stepResult.setTestStep(step);
                    stepResult.setStepNumber(step.getStepNumber());
                    stepResult.setStatus("NOT_EXECUTED");
                    stepResult.setActualResult("");
                    stepResults.add(stepResult);
                }
                execution.setStepResults(stepResults);
            }

            TestExecution savedExecution = testExecutionRepository.save(execution);
            entityManager.flush();
            return savedExecution;
        }
        throw new RuntimeException("Test case or user not found with id: " + testCaseId + " or " + userId);
    }

    /**
     * Convert TestExecution to DTO with proper null safety.
     */
    private TestExecutionDTO convertToDTO(TestExecution execution) {
        TestCase testCase = execution.getTestCase();
        
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

        // Safe navigation for nested properties
        Long testModuleId = null;
        String testModuleName = "";
        Long projectId = null;
        String projectName = "";
        
        if (testCase != null) {
            TestModule testModule = testCase.getTestModule();
            if (testModule != null) {
                testModuleId = testModule.getId();
                testModuleName = testModule.getName() != null ? testModule.getName() : "";
                
                Project project = testModule.getProject();
                if (project != null) {
                    projectId = project.getId();
                    projectName = project.getName() != null ? project.getName() : "";
                }
            }
        }

        return new TestExecutionDTO(
            execution.getId(),
            execution.getTestCaseId(),
            testCase != null && testCase.getTitle() != null ? testCase.getTitle() : "",
            execution.getExecutionDate(),
            execution.getOverallResult(),
            execution.getNotes(),
            execution.getDuration(),
            execution.getEnvironment(),
            execution.getExecutedBy(),
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getId() : null,
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getUsername() : "",
            testCase != null ? testCase.getSubmoduleId() : null,
            testCase != null ? testCase.getSubmoduleName() : null,
            testModuleId,
            testModuleName,
            projectId,
            projectName,
            stepResultDTOs
        );
    }
}
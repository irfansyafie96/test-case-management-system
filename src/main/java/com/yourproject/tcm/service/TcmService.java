package com.yourproject.tcm.service;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.StepResultResponse;
import com.yourproject.tcm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TCM Service - Main Business Logic Layer for the Test Case Management System
 *
 * This service class contains all the business logic for managing the test case hierarchy:
 * Projects → Modules → Suites → Test Cases → Test Steps, and their executions/results.
 *
 * Key Responsibilities:
 * 1. CRUD operations for all entities (Create, Read, Update, Delete)
 * 2. Maintaining data integrity across the hierarchy
 * 3. Managing relationships between entities
 * 4. Handling cascading operations (e.g., deleting a project deletes all its contents)
 * 5. Creating and managing test executions and their results
 *
 * Transactional: All methods are wrapped in database transactions to ensure data consistency
 */
@Service
@Transactional  // All methods in this service are transactional by default
public class TcmService {

    @Autowired
    private EntityManager entityManager;  // Direct access to JPA EntityManager for manual DB operations

    @Autowired
    private ProjectRepository projectRepository;  // Repository for Project operations

    @Autowired
    private TestModuleRepository testModuleRepository;  // Repository for TestModule operations

    @Autowired
    private TestSuiteRepository testSuiteRepository;  // Repository for TestSuite operations

    @Autowired
    private TestCaseRepository testCaseRepository;  // Repository for TestCase operations

    @Autowired
    private TestExecutionRepository testExecutionRepository;  // Repository for TestExecution operations

    @Autowired
    private TestStepResultRepository testStepResultRepository;  // Repository for TestStepResult operations

    // ==================== PROJECT METHODS ====================

    /**
     * Get all projects in the system
     * @return List of all projects
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Create a new project
     * @param project The project to create
     * @return The created project
     * @throws RuntimeException if a project with the same name already exists
     */
    @Transactional
    public Project createProject(Project project) {
        // Check if a project with the same name already exists to provide a better error message
        Optional<Project> existingProject = projectRepository.findByName(project.getName());
        if (existingProject.isPresent()) {
            throw new RuntimeException("A project with name '" + project.getName() + "' already exists");
        }
        return projectRepository.save(project);
    }

    /**
     * Get a specific project by ID
     * @param projectId The ID of the project to retrieve
     * @return Optional containing the project, or empty if not found
     */
    @Transactional(readOnly = true)  // Read-only transaction for better performance
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * Delete a project and all its contents (cascading delete)
     * This will delete: Project → Modules → Suites → Test Cases → Executions → Step Results
     * @param projectId The ID of the project to delete
     * @throws RuntimeException if project doesn't exist
     */
    @Transactional
    public void deleteProject(Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();

            // Delete all test modules associated with this project
            // This will cascade to delete all related test suites, test cases, executions and step results
            if (project.getModules() != null) {
                for (TestModule module : project.getModules()) {
                    deleteTestModule(module.getId());  // Recursively delete module and its contents
                }
            }

            // Now delete the project itself
            projectRepository.deleteById(projectId);
            entityManager.flush(); // Ensure data is written to DB
        } else {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
    }

    // ==================== TEST MODULE METHODS ====================

    /**
     * Get a specific test module by ID with all its test suites and test cases
     * @param testModuleId The ID of the test module to retrieve
     * @return Optional containing the test module, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<TestModule> getTestModuleById(Long testModuleId) {
        Optional<TestModule> testModuleOpt = testModuleRepository.findByIdWithTestSuites(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();

            // Fetch all test suites with their test cases for this module in a single query
            List<TestSuite> suitesWithTestCases = testSuiteRepository.findByTestModuleIdWithTestCases(testModuleId);

            // Create a map for easier lookup
            Map<Long, TestSuite> suiteMap = suitesWithTestCases.stream()
                .collect(Collectors.toMap(TestSuite::getId, suite -> suite));

            // Update the test suites in the module with the ones that have test cases loaded
            if (testModule.getTestSuites() != null) {
                for (TestSuite testSuite : testModule.getTestSuites()) {
                    TestSuite suiteWithTestCases = suiteMap.get(testSuite.getId());
                    if (suiteWithTestCases != null) {
                        testSuite.setTestCases(suiteWithTestCases.getTestCases());
                    }
                }
            }
            return Optional.of(testModule);
        }
        return testModuleOpt;
    }

    /**
     * Create a new test module within a specific project
     * @param projectId The project ID to add the module to
     * @param testModule The test module to create
     * @return The created test module
     * @throws RuntimeException if project doesn't exist
     */
    @Transactional
    public TestModule createTestModuleForProject(Long projectId, TestModule testModule) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            testModule.setProject(project);  // Set the project relationship
            TestModule savedTestModule = testModuleRepository.save(testModule);
            entityManager.flush(); // Ensure data is written to DB
            return savedTestModule;
        } else {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
    }

    /**
     * Update an existing test module
     * @param testModuleId The ID of the module to update
     * @param testModuleDetails Updated module details
     * @return The updated test module
     * @throws RuntimeException if module doesn't exist
     */
    @Transactional
    public TestModule updateTestModule(Long testModuleId, TestModule testModuleDetails) {
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();
            testModule.setName(testModuleDetails.getName());  // Only update name currently
            TestModule updatedTestModule = testModuleRepository.save(testModule);
            entityManager.flush(); // Ensure data is written to DB
            return updatedTestModule;
        } else {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
    }

    /**
     * Delete a test module and all its contents (cascading delete)
     * This will delete: Module → Suites → Test Cases → Executions → Step Results
     * @param testModuleId The ID of the test module to delete
     * @throws RuntimeException if module doesn't exist
     */
    @Transactional
    public void deleteTestModule(Long testModuleId) {
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();

            // First, delete all test executions for test cases in this module
            // This is necessary to avoid foreign key constraint violations
            if (testModule.getTestSuites() != null) {
                for (TestSuite suite : testModule.getTestSuites()) {
                    if (suite.getTestCases() != null) {
                        for (TestCase testCase : suite.getTestCases()) {
                            // Delete executions for this test case
                            List<TestExecution> executions = testExecutionRepository.findByTestCaseId(testCase.getId());
                            for (TestExecution execution : executions) {
                                testExecutionRepository.deleteById(execution.getId());
                            }

                            // Delete test step results for test steps in this test case
                            if (testCase.getTestSteps() != null) {
                                for (TestStep step : testCase.getTestSteps()) {
                                    testStepResultRepository.deleteByTestStepId(step.getId());
                                }
                            }
                        }
                    }
                }
            }

            // Now delete the test module (cascading should handle test suites, test cases, and test steps)
            testModuleRepository.deleteById(testModuleId);
            entityManager.flush(); // Ensure data is written to DB
        } else {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
    }

    // ==================== TEST SUITE METHODS ====================

    /**
     * Get a specific test suite by ID with its module information
     * @param suiteId The ID of the test suite to retrieve
     * @return Optional containing the test suite, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<TestSuite> getTestSuiteById(Long suiteId) {
        return testSuiteRepository.findByIdWithModule(suiteId);
    }

    /**
     * Create a new test suite within a specific test module
     * @param testModuleId The module ID to add the suite to
     * @param testSuite The test suite to create
     * @return The created test suite
     * @throws RuntimeException if module doesn't exist
     */
    @Transactional
    public TestSuite createTestSuiteForTestModule(Long testModuleId, TestSuite testSuite) {
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();
            testSuite.setTestModule(testModule);  // Set the module relationship
            TestSuite savedTestSuite = testSuiteRepository.save(testSuite);
            entityManager.flush(); // Ensure data is written to DB
            return savedTestSuite;
        } else {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
    }

    /**
     * Update an existing test suite
     * @param suiteId The ID of the suite to update
     * @param suiteDetails Updated suite details
     * @return The updated test suite
     * @throws RuntimeException if suite doesn't exist
     */
    @Transactional
    public TestSuite updateTestSuite(Long suiteId, TestSuite suiteDetails) {
        Optional<TestSuite> suiteOpt = testSuiteRepository.findById(suiteId);
        if (suiteOpt.isPresent()) {
            TestSuite testSuite = suiteOpt.get();
            testSuite.setName(suiteDetails.getName());  // Only update name currently
            TestSuite updatedTestSuite = testSuiteRepository.save(testSuite);
            entityManager.flush(); // Ensure data is written to DB
            return updatedTestSuite;
        } else {
            throw new RuntimeException("Test Suite not found with id: " + suiteId);
        }
    }

    // ==================== TEST CASE METHODS ====================

    /**
     * Get all test cases in the system
     * @return List of all test cases
     */
    @Transactional(readOnly = true)
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    /**
     * Get a specific test case by ID
     * @param testCaseId The ID of the test case to retrieve
     * @return Optional containing the test case, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<TestCase> getTestCaseById(Long testCaseId) {
        return testCaseRepository.findById(testCaseId);
    }

    /**
     * Create a new test case within a specific test suite
     * @param suiteId The suite ID to add the test case to
     * @param testCase The test case to create (with its test steps)
     * @return The created test case
     * @throws RuntimeException if suite doesn't exist
     */
    @Transactional
    public TestCase createTestCaseForTestSuite(Long suiteId, TestCase testCase) {
        Optional<TestSuite> suiteOpt = testSuiteRepository.findById(suiteId);
        if (suiteOpt.isPresent()) {
            TestSuite testSuite = suiteOpt.get();
            testCase.setTestSuite(testSuite);  // Set the suite relationship

            // If the test case has steps, set up the relationship and step numbers
            if (testCase.getTestSteps() != null) {
                int stepNum = 1;
                for (TestStep step : testCase.getTestSteps()) {
                    step.setTestCase(testCase);      // Set the back-reference
                    step.setStepNumber(stepNum++);   // Assign sequential step numbers (1, 2, 3, etc.)
                }
            }
            TestCase savedTestCase = testCaseRepository.save(testCase);
            entityManager.flush(); // Ensure data is written to DB
            entityManager.clear(); // Clear the persistence context to avoid stale data
            return savedTestCase;
        } else {
            throw new RuntimeException("Test Suite not found with id: " + suiteId);
        }
    }

    /**
     * Update an existing test case and its steps
     * @param testCaseId The ID of the test case to update
     * @param testCaseDetails Updated test case details (including steps)
     * @return The updated test case
     * @throws RuntimeException if test case doesn't exist
     */
    @Transactional
    public TestCase updateTestCase(Long testCaseId, TestCase testCaseDetails) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();

            // Update basic properties (ID, title, testCaseId)
            testCase.setTitle(testCaseDetails.getTitle());
            testCase.setTestCaseId(testCaseDetails.getTestCaseId());

            // Handle test steps - properly manage the relationship to avoid cascade issues
            if (testCaseDetails.getTestSteps() != null) {
                // Get or create the current test steps list to maintain the same collection instance
                List<TestStep> currentSteps = testCase.getTestSteps();
                if (currentSteps == null) {
                    currentSteps = new ArrayList<>();
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
                List<TestStep> currentSteps = testCase.getTestSteps();
                if (currentSteps != null) {
                    // Delete related test step results first to avoid foreign key constraint violations
                    for (TestStep existingStep : currentSteps) {
                        testStepResultRepository.deleteByTestStepId(existingStep.getId());
                    }

                    // Clear the existing test steps
                    currentSteps.clear();
                }
            }

            TestCase updatedTestCase = testCaseRepository.save(testCase);
            entityManager.flush(); // Ensure data is written to DB
            return updatedTestCase;
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    /**
     * Delete a test case and all its executions/step results
     * @param testCaseId The ID of the test case to delete
     * @throws RuntimeException if test case doesn't exist
     */
    @Transactional
    public void deleteTestCase(Long testCaseId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();

            // First, delete all test executions for this test case
            // This will cascade to delete test step results associated with those executions
            List<TestExecution> executions = testExecutionRepository.findByTestCaseId(testCaseId);
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
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    // ==================== TEST EXECUTION METHODS ====================

    /**
     * Get a specific test execution by ID with all its step results
     * @param executionId The ID of the test execution to retrieve
     * @return Optional containing the test execution, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<TestExecution> getTestExecutionById(Long executionId) {
        return testExecutionRepository.findByIdWithStepResults(executionId);
    }

    /**
     * Get all executions of a specific test case
     * @param testCaseId The ID of the test case
     * @return List of all executions for that test case
     */
    @Transactional(readOnly = true)
    public List<TestExecution> getTestExecutionsByTestCaseId(Long testCaseId) {
        return testExecutionRepository.findByTestCaseId(testCaseId);
    }

    /**
     * Create a new test execution for a specific test case
     * This creates an execution record and step result records for all steps in the test case
     * @param testCaseId The ID of the test case to execute
     * @return The created test execution with empty step results
     * @throws RuntimeException if test case doesn't exist
     */
    @Transactional
    public TestExecution createTestExecutionForTestCase(Long testCaseId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();

            // Create the main execution record
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);  // Link to the test case
            execution.setExecutionDate(LocalDateTime.now());  // Set current time
            execution.setOverallResult("Incomplete");  // Default to incomplete until finished

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
                        result.setStatus("Skipped");  // Default to skipped until executed
                        return result;
                    })
                    .collect(Collectors.toList());

                // Assign the step results to the execution and save again
                initialExecution.setStepResults(stepResults);
                TestExecution finalExecution = testExecutionRepository.save(initialExecution);
                return finalExecution;
            }

            entityManager.flush();
            return initialExecution;
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    /**
     * Update the result of a specific step in a test execution
     * @param executionId The ID of the execution
     * @param stepId The ID of the step
     * @param status The new status ("Pass", "Fail", "Skipped")
     * @param actualResult What actually happened when the step was executed
     * @return StepResultResponse DTO with updated information
     * @throws RuntimeException if step result doesn't exist for the execution/step combination
     */
    @Transactional
    public com.yourproject.tcm.model.dto.StepResultResponse updateStepResult(Long executionId, Long stepId, String status, String actualResult) {
        TestStepResult existingResult = testStepResultRepository.findByTestExecutionIdAndTestStepId(executionId, stepId);
        if (existingResult != null) {
            existingResult.setStatus(status);       // Update status
            existingResult.setActualResult(actualResult);  // Update actual result
            TestStepResult savedResult = testStepResultRepository.save(existingResult);
            entityManager.flush();

            // Return DTO to avoid serialization issues (circular references)
            return new com.yourproject.tcm.model.dto.StepResultResponse(
                savedResult.getId(),
                savedResult.getTestStep().getId(),
                savedResult.getStepNumber(),
                savedResult.getActualResult(),
                savedResult.getStatus(),
                savedResult.getTestStep().getAction(),
                savedResult.getTestStep().getExpectedResult()
            );
        } else {
            throw new RuntimeException("Step result not found for execution " + executionId + " and step " + stepId);
        }
    }

    /**
     * Complete a test execution by setting the overall result and notes
     * @param executionId The ID of the execution to complete
     * @param overallResult The overall result ("Pass", "Fail", etc.)
     * @param notes Any notes about the execution
     * @return The completed test execution
     * @throws RuntimeException if execution doesn't exist
     */
    @Transactional
    public TestExecution completeTestExecution(Long executionId, String overallResult, String notes) {
        Optional<TestExecution> executionOpt = testExecutionRepository.findById(executionId);
        if (executionOpt.isPresent()) {
            TestExecution execution = executionOpt.get();
            execution.setOverallResult(overallResult);  // Set the final result
            execution.setNotes(notes);  // Add any notes
            TestExecution savedExecution = testExecutionRepository.save(execution);
            entityManager.flush();
            return savedExecution;
        } else {
            throw new RuntimeException("Test Execution not found with id: " + executionId);
        }
    }
}

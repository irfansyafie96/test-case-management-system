package com.yourproject.tcm.service;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.ModuleAssignmentRequest;
import com.yourproject.tcm.model.dto.ProjectAssignmentRequest;
import com.yourproject.tcm.model.dto.TestAnalyticsDTO;
import com.yourproject.tcm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

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
    private UserRepository userRepository;  // Repository for User operations

    @Autowired
    private TestStepResultRepository testStepResultRepository;  // Repository for TestStepResult operations

    // Helper methods for assignment-based filtering
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
        }
        throw new RuntimeException("No authenticated user found");
    }

    public String getCurrentUserOrganization() {
        String org = getCurrentUser().getOrganization();
        return org != null ? org : "default";
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
    }

    private boolean isQaOrBa(User user) {
        return user.getRoles().stream().anyMatch(role -> 
            role.getName().equals("QA") || role.getName().equals("BA"));
    }

    private boolean isTester(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals("TESTER"));
    }

    // ==================== PROJECT METHODS ====================

    /**
     * Get all projects in the system
     * For ADMIN users: returns all projects
     * For QA/BA/TESTER users: returns projects they have ANY access to (Direct or via Module)
     * @return List of projects based on user role and assignments
     */
    public List<Project> getAllProjects() {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) {
            return projectRepository.findAll();
        } else {
            return projectRepository.findProjectsAssignedToUser(currentUser.getId());
        }
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
     * For ADMIN users: returns project if it exists
     * For QA/BA users: returns project only if assigned to them
     * @param projectId The ID of the project to retrieve
     * @return Optional containing the project, or empty if not found/not assigned
     */
    @Transactional(readOnly = true)  // Read-only transaction for better performance
    public Optional<Project> getProjectById(Long projectId) {
        User currentUser = getCurrentUser();
        Optional<Project> projectOpt = projectRepository.findProjectWithModulesById(projectId);
        
        if (!projectOpt.isPresent()) {
            return Optional.empty();
        }

        // Force initialization of lazy collections
        Project project = projectOpt.get();
        if (project.getModules() != null) {
            for (TestModule module : project.getModules()) {
                // Accessing the collection forces Hibernate to load it
                if (module.getTestSuites() != null) {
                    module.getTestSuites().size(); 
                    for (TestSuite suite : module.getTestSuites()) {
                        if (suite.getTestCases() != null) {
                            suite.getTestCases().size();
                        }
                    }
                }
            }
        }
        
        if (isAdmin(currentUser)) {
            return projectOpt;
        } else {
            // Check if project is assigned to the user (Directly or via Module)
            List<Project> assignedProjects = projectRepository.findProjectsAssignedToUser(currentUser.getId());
            boolean isAssigned = assignedProjects.stream().anyMatch(p -> p.getId().equals(projectId));
            return isAssigned ? projectOpt : Optional.empty();
        }
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
        // Fetch the module WITHOUT its test suites to avoid orphanRemoval issues
        // orphanRemoval=true on TestModule.testSuites causes JPA to try nullifying foreign key
        // which violates NOT NULL constraint. We'll delete suites separately.
        TestModule testModule = testModuleRepository.findById(testModuleId)
                .orElseThrow(() -> new RuntimeException("Test Module not found with id: " + testModuleId));

        // Fetch test cases with their test steps for cleanup
        // Using a separate query to avoid complex multi-level JOIN FETCH that causes JPA errors
        List<TestCase> testCasesWithSteps = testCaseRepository.findByModuleIdWithSteps(testModuleId);

        // Pre-clean up: Delete all operational data (Executions and Results) linked to these test cases.
        // This is necessary because TestStepResult has a Foreign Key to TestStep.
        // If we try to delete TestModule -> TestSuite -> TestCase -> TestStep directly,
        // the DB will throw a constraint violation because TestStepResult still points to TestStep.
        if (testCasesWithSteps != null) {
            for (TestCase testCase : testCasesWithSteps) {
                // 1. Delete all TestExecutions for this test case.
                // This cascades to delete TestStepResults linked to these executions.
                List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCase.getId());
                if (!executions.isEmpty()) {
                    testExecutionRepository.deleteAll(executions);
                }

                // 2. Safety Net: Delete any orphaned TestStepResults that might point to these steps
                // but aren't linked to a valid execution (data integrity cleanup).
                if (testCase.getTestSteps() != null) {
                    for (TestStep step : testCase.getTestSteps()) {
                        testStepResultRepository.deleteByTestStepId(step.getId());
                    }
                }
            }
        }
        
        // Flush the changes to ensure all dependent data is gone before we delete the structure
        entityManager.flush();

        // Fetch test suites separately (not as part of module's collection)
        // This avoids triggering orphanRemoval when we delete them
        List<TestSuite> testSuites = testSuiteRepository.findByTestModule_Id(testModuleId);
        
        // Delete each test suite separately, which will cascade to delete its test cases and steps
        // Using testSuiteRepository.delete() instead of entityManager.remove() to work with JPA properly
        if (testSuites != null && !testSuites.isEmpty()) {
            for (TestSuite suite : testSuites) {
                testSuiteRepository.delete(suite);
            }
            entityManager.flush();
            
            // Clear the testSuites collection from the module entity if it was loaded
            // This prevents any orphanRemoval attempts later
            if (testModule.getTestSuites() != null) {
                testModule.getTestSuites().clear();
            }
        }

        // Now delete the module structure (test suites already deleted)
        testModuleRepository.delete(testModule);
        entityManager.flush(); // Final commit
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

    /**
     * Delete a test suite by ID
     * This will also delete all test cases in the suite (cascading)
     * @param suiteId The ID of the test suite to delete
     * @throws RuntimeException if suite doesn't exist
     */
    @Transactional
    public void deleteTestSuite(Long suiteId) {
        Optional<TestSuite> suiteOpt = testSuiteRepository.findById(suiteId);
        if (suiteOpt.isPresent()) {
            TestSuite testSuite = suiteOpt.get();

            // First, delete all test cases in the suite
            // This will cascade to delete test steps, test executions, and test step results
            if (testSuite.getTestCases() != null) {
                for (TestCase testCase : testSuite.getTestCases()) {
                    // Delete all test executions for this test case
                    List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCase.getId());
                    for (TestExecution execution : executions) {
                        testExecutionRepository.deleteById(execution.getId());
                    }

                    // Delete test step results that might still reference the test steps
                    if (testCase.getTestSteps() != null) {
                        for (TestStep step : testCase.getTestSteps()) {
                            testStepResultRepository.deleteByTestStepId(step.getId());
                        }
                    }

                    // Delete the test case
                    testCaseRepository.deleteById(testCase.getId());
                }
            }

            // Now delete the test suite
            testSuiteRepository.deleteById(suiteId);
            entityManager.flush(); // Ensure data is written to DB
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
        return testCaseRepository.findAllWithDetails();
    }

    /**
     * Get test execution analytics
     * Calculates pass/fail statistics for all test cases
     * @param userId Optional user ID to filter executions (admin only). For non-admin users, only their own executions are shown.
     * @return TestAnalyticsDTO containing overall KPIs and breakdown by project/module
     */
    @Transactional(readOnly = true)
    public TestAnalyticsDTO getTestAnalytics(Long userId) {
        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final User currentUser;
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            currentUser = userRepository.findByUsername(username).orElse(null);
        } else {
            currentUser = null;
        }

        // Determine which user's executions to show
        final Long filterUserId;
        if (currentUser == null) {
            // No authenticated user - return empty analytics
            return new TestAnalyticsDTO(0, 0, 0, 0, 0, 0.0, 0.0, new ArrayList<>(), new ArrayList<>());
        } else if (isAdmin(currentUser)) {
            // Admin can filter by userId or see all
            filterUserId = userId;
        } else {
            // Non-admin users only see their own executions
            filterUserId = currentUser.getId();
        }

        // Get all test cases with their test suites and modules (eagerly fetched)
        List<TestCase> allTestCases = testCaseRepository.findAllWithDetails();

        // Get all executions with details
        List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetails();

        // Filter executions by user if filterUserId is set
        if (filterUserId != null) {
            allExecutions = allExecutions.stream()
                .filter(execution -> {
                    User assignedUser = execution.getAssignedToUser();
                    return assignedUser != null && assignedUser.getId().equals(filterUserId);
                })
                .collect(Collectors.toList());
            
            // For non-admin users, also filter by assigned modules (match execution page logic)
            if (!isAdmin(currentUser)) {
                Set<Long> assignedModuleIds = currentUser.getAssignedTestModules().stream()
                    .map(TestModule::getId)
                    .collect(Collectors.toSet());
                
                allExecutions = allExecutions.stream()
                    .filter(execution -> {
                        Long moduleId = execution.getModuleId();
                        return moduleId != null && assignedModuleIds.contains(moduleId);
                    })
                    .collect(Collectors.toList());
            }
        }

        // Track which test cases have been executed
        Set<Long> executedTestCaseIds = new HashSet<>();
        Map<Long, TestExecution> latestExecutionByTestCase = new HashMap<>();
        Map<Long, String> latestExecutionResults = new HashMap<>(); // testCaseId -> result

        // Valid execution results that indicate completed execution
        Set<String> completedResults = Set.of("PASSED", "FAILED", "BLOCKED", "PARTIALLY_PASSED");

        // Find the latest execution for each test case
        for (TestExecution execution : allExecutions) {
            Long testCaseId = execution.getTestCase().getId();
            
            // Only count executions that have been completed (not PENDING)
            String result = execution.getOverallResult();
            if (result != null && completedResults.contains(result.toUpperCase())) {
                executedTestCaseIds.add(testCaseId);

                // Keep the latest execution (by date)
                if (!latestExecutionByTestCase.containsKey(testCaseId) ||
                    execution.getExecutionDate().isAfter(latestExecutionByTestCase.get(testCaseId).getExecutionDate())) {
                    latestExecutionByTestCase.put(testCaseId, execution);
                }
            }
        }

        // Extract results from latest executions
        for (TestExecution execution : latestExecutionByTestCase.values()) {
            latestExecutionResults.put(execution.getTestCase().getId(), execution.getOverallResult());
        }

        // Filter test cases based on user assignments and executions
        List<TestCase> filteredTestCases = new ArrayList<>();
        if (isAdmin(currentUser)) {
            if (filterUserId == null) {
                // Admin seeing all test cases (no user filter)
                filteredTestCases = allTestCases;
            } else {
                // Admin filtering by specific user - only show test cases that user has executed
                filteredTestCases = allTestCases.stream()
                    .filter(tc -> executedTestCaseIds.contains(tc.getId()))
                    .collect(Collectors.toList());
            }
        } else {
            // Non-admin users see ONLY test cases they have executions for (Assigned to them)
            // allExecutions is already filtered to only include executions assigned to this user
            Set<Long> userTestCaseIds = allExecutions.stream()
                .map(ex -> ex.getTestCase().getId())
                .collect(Collectors.toSet());

            filteredTestCases = allTestCases.stream()
                .filter(tc -> userTestCaseIds.contains(tc.getId()))
                .collect(Collectors.toList());
            
            // Debug logging
            System.out.println("=== Analytics Debug for user: " + currentUser.getUsername() + " ===");
            System.out.println("Total allTestCases: " + allTestCases.size());
            System.out.println("Total allExecutions (before filter): " + testExecutionRepository.findAllWithDetails().size());
            System.out.println("Filtered executions (after user & module filter): " + allExecutions.size());
            System.out.println("executedTestCaseIds: " + executedTestCaseIds);
            System.out.println("filteredTestCases (final): " + filteredTestCases.size());
            System.out.println("User assigned projects: " + currentUser.getAssignedProjects().size());
            System.out.println("User assigned modules: " + currentUser.getAssignedTestModules().size());
            System.out.println("Assigned module IDs: " + currentUser.getAssignedTestModules().stream().map(TestModule::getId).collect(Collectors.toList()));
        }

        // Calculate overall KPIs
        // filteredTestCases contains:
        // - Admin (no filter): all test cases
        // - Admin (with filter): only test cases that user has executed
        // - Non-admin: all test cases in assigned modules
        long totalTestCases = filteredTestCases.size();
        
        // For non-admin users, calculate executedCount from filteredTestCases that have executions
        // For admin users, use executedTestCaseIds directly
        long executedCount;
        if (isAdmin(currentUser)) {
            executedCount = executedTestCaseIds.size();
        } else {
            // Count how many test cases in filteredTestCases have executions
            executedCount = filteredTestCases.stream()
                .filter(tc -> executedTestCaseIds.contains(tc.getId()))
                .count();
        }
        
        long notExecutedCount = totalTestCases - executedCount;

        long passedCount = 0;
        long failedCount = 0;

        for (String result : latestExecutionResults.values()) {
            if ("Pass".equalsIgnoreCase(result)) {
                passedCount++;
            } else if ("Fail".equalsIgnoreCase(result)) {
                failedCount++;
            }
        }

        double passRate = executedCount > 0 ? (passedCount * 100.0 / executedCount) : 0.0;
        double failRate = executedCount > 0 ? (failedCount * 100.0 / executedCount) : 0.0;

        // Calculate project breakdown
        Map<Long, TestAnalyticsDTO.ProjectAnalytics> projectStats = new HashMap<>();
        Map<Long, TestAnalyticsDTO.ModuleAnalytics> moduleStats = new HashMap<>();

        for (TestCase testCase : filteredTestCases) {
            // Get project info
            TestSuite suite = testCase.getTestSuite();
            if (suite == null || suite.getTestModule() == null) continue;

            TestModule module = suite.getTestModule();
            Project project = module.getProject();
            if (project == null) continue;

            Long projectId = project.getId();
            String projectName = project.getName();
            Long moduleId = module.getId();
            String moduleName = module.getName();

            // Initialize project stats if needed
            projectStats.putIfAbsent(projectId, new TestAnalyticsDTO.ProjectAnalytics(
                projectId, projectName, 0, 0, 0, 0, 0
            ));

            // Initialize module stats if needed
            moduleStats.putIfAbsent(moduleId, new TestAnalyticsDTO.ModuleAnalytics(
                moduleId, moduleName, projectId, projectName, 0, 0, 0, 0, 0
            ));

            // Check if this test case has been executed
            boolean isExecuted = executedTestCaseIds.contains(testCase.getId());

            // Update project stats
            TestAnalyticsDTO.ProjectAnalytics pStats = projectStats.get(projectId);
            pStats.setTotalTestCases(pStats.getTotalTestCases() + 1);

            if (isExecuted) {
                pStats.setExecutedCount(pStats.getExecutedCount() + 1);
                String result = latestExecutionResults.get(testCase.getId());
                if ("Pass".equalsIgnoreCase(result)) {
                    pStats.setPassedCount(pStats.getPassedCount() + 1);
                } else if ("Fail".equalsIgnoreCase(result)) {
                    pStats.setFailedCount(pStats.getFailedCount() + 1);
                }
            } else {
                pStats.setNotExecutedCount(pStats.getNotExecutedCount() + 1);
            }

            // Update module stats
            TestAnalyticsDTO.ModuleAnalytics mStats = moduleStats.get(moduleId);
            mStats.setTotalTestCases(mStats.getTotalTestCases() + 1);

            if (isExecuted) {
                mStats.setExecutedCount(mStats.getExecutedCount() + 1);
                String result2 = latestExecutionResults.get(testCase.getId());
                if ("Pass".equalsIgnoreCase(result2)) {
                    mStats.setPassedCount(mStats.getPassedCount() + 1);
                } else if ("Fail".equalsIgnoreCase(result2)) {
                    mStats.setFailedCount(mStats.getFailedCount() + 1);
                }
            } else {
                mStats.setNotExecutedCount(mStats.getNotExecutedCount() + 1);
            }
        }

        return new TestAnalyticsDTO(
            totalTestCases, executedCount, passedCount, failedCount, notExecutedCount,
            passRate, failRate,
            new ArrayList<>(projectStats.values()),
            new ArrayList<>(moduleStats.values())
        );
    }

    /**
     * Get a specific test case by ID
     * @param testCaseId The ID of the test case to retrieve
     * @return Optional containing the test case, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<TestCase> getTestCaseById(Long testCaseId) {
        TestCase testCase = testCaseRepository.findByIdWithSteps(testCaseId);
        return Optional.ofNullable(testCase);
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

            // NOTE: Auto-generating executions for all module users has been disabled
            // Executions should only be created when explicitly assigned to a specific user
            // This allows QA/BA to create test cases without automatically getting executions

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
            List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCaseId);
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
        return testExecutionRepository.findByTestCase_Id(testCaseId);
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
            execution.setOverallResult("PENDING");  // Default to pending until executed

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
        TestStepResult existingResult = testStepResultRepository.findByTestExecution_IdAndTestStep_Id(executionId, stepId);
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
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
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

    /**
     * Assign a test execution to a specific user
     * @param executionId ID of the execution to assign
     * @param userId ID of the user to assign to
     * @return The updated test execution
     */
    @Transactional
    public TestExecution assignTestExecutionToUser(Long executionId, Long userId) {
        Optional<TestExecution> executionOpt = testExecutionRepository.findByIdWithStepResults(executionId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (executionOpt.isPresent() && userOpt.isPresent()) {
            TestExecution execution = executionOpt.get();
            User user = userOpt.get();

            // Check if user has QA, BA, or TESTER role
            boolean hasValidRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("QA") || role.getName().equals("BA") || role.getName().equals("TESTER"));
            if (!hasValidRole) {
                throw new RuntimeException("User must have QA, BA, or TESTER role to be assigned to test executions");
            }

            // Check if user belongs to same organization as current user
            String currentUserOrganization = getCurrentUser().getOrganization();
            if (currentUserOrganization == null) {
                currentUserOrganization = "default";
            }
            String userOrganization = user.getOrganization();
            if (userOrganization == null) {
                userOrganization = "default";
            }
            if (!userOrganization.equals(currentUserOrganization)) {
                throw new RuntimeException("User must belong to the same organization as the assigner");
            }

            execution.setAssignedToUser(user);
            TestExecution savedExecution = testExecutionRepository.save(execution);
            entityManager.flush();
            return savedExecution;
        }
        throw new RuntimeException("Test execution or user not found with id: " + executionId + " or " + userId);
    }

    /**
     * Get all test executions assigned to a specific user
     * @param userId ID of the user
     * @return List of assigned test executions
     */
    public List<TestExecution> getTestExecutionsAssignedToUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return testExecutionRepository.findByAssignedToUserWithDetails(user);
        }
        throw new RuntimeException("User not found with id: " + userId);
    }

    /**
     * Get all test executions assigned to the current authenticated user
     * @return List of assigned test executions
     */
    public List<TestExecution> getTestExecutionsForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            // If user is ADMIN, return all executions but only one per test case (latest)
            if (isAdmin(user)) {
                List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetails();
                Map<Long, TestExecution> latestExecutionByTestCase = new HashMap<>();
                
                for (TestExecution execution : allExecutions) {
                    Long testCaseId = execution.getTestCase().getId();
                    // Keep only the latest execution for each test case
                    if (!latestExecutionByTestCase.containsKey(testCaseId) ||
                        execution.getExecutionDate().isAfter(latestExecutionByTestCase.get(testCaseId).getExecutionDate())) {
                        latestExecutionByTestCase.put(testCaseId, execution);
                    }
                }
                return new ArrayList<>(latestExecutionByTestCase.values());
            }
            
            // Otherwise return only executions assigned to the user for modules they're currently assigned to
            List<TestExecution> allAssignedExecutions = testExecutionRepository.findByAssignedToUserWithDetails(user);
            Set<Long> assignedModuleIds = user.getAssignedTestModules().stream()
                .map(com.yourproject.tcm.model.TestModule::getId)
                .collect(java.util.stream.Collectors.toSet());

            return allAssignedExecutions.stream()
                .filter(execution -> {
                    Long moduleId = execution.getModuleId();
                    return moduleId != null && assignedModuleIds.contains(moduleId);
                })
                .collect(java.util.stream.Collectors.toList());
        }
        throw new RuntimeException("No authenticated user found");
    }

    // ==================== PROJECT ASSIGNMENT METHODS ====================

    /**
     * Assign a QA/BA user to a project
     * @param request Project assignment request containing userId and projectId
     * @return The updated user with assigned projects
     */
    @Transactional
    public User assignUserToProject(ProjectAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());

        if (userOpt.isPresent() && projectOpt.isPresent()) {
            User user = userOpt.get();
            Project project = projectOpt.get();

            // Check if user has QA or BA role
            boolean hasQaOrBaRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("QA") || role.getName().equals("BA"));
            
            if (!hasQaOrBaRole) {
                throw new RuntimeException("User must have QA or BA role to be assigned to projects");
            }

            // Check if user belongs to same organization as current user
            String currentUserOrganization = getCurrentUser().getOrganization();
            if (currentUserOrganization == null) {
                currentUserOrganization = "default";
            }
            String userOrganization = user.getOrganization();
            if (userOrganization == null) {
                userOrganization = "default";
            }
            if (!userOrganization.equals(currentUserOrganization)) {
                throw new RuntimeException("User must belong to the same organization as the assigner");
            }

            // Add project to user's assigned projects if not already assigned
            if (!user.getAssignedProjects().contains(project)) {
                user.getAssignedProjects().add(project);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Already assigned
            }
        }
        throw new RuntimeException("User or project not found with id: " + request.getUserId() + " or " + request.getProjectId());
    }

    /**
     * Remove a QA/BA user from a project assignment
     * @param request Project assignment request containing userId and projectId
     * @return The updated user
     */
    @Transactional
    public User removeUserFromProject(ProjectAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());

        if (userOpt.isPresent() && projectOpt.isPresent()) {
            User user = userOpt.get();
            Project project = projectOpt.get();

            // Remove project from user's assigned projects
            if (user.getAssignedProjects().contains(project)) {
                user.getAssignedProjects().remove(project);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Not assigned
            }
        }
        throw new RuntimeException("User or project not found with id: " + request.getUserId() + " or " + request.getProjectId());
    }

    /**
     * Get all projects assigned to the current authenticated user
     * @return List of assigned projects
     */
    public List<Project> getProjectsAssignedToCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            return projectRepository.findProjectsAssignedToUser(user.getId());
        }
        throw new RuntimeException("No authenticated user found");
    }

    // ==================== MODULE ASSIGNMENT METHODS ====================

    /**
     * Assign a TESTER (or QA/BA) user to a test module
     * @param request Module assignment request containing userId and testModuleId
     * @return The updated user with assigned test modules
     */
    @Transactional
    public User assignUserToTestModule(ModuleAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<TestModule> moduleOpt = testModuleRepository.findById(request.getTestModuleId());

        if (userOpt.isPresent() && moduleOpt.isPresent()) {
            User user = userOpt.get();
            TestModule module = moduleOpt.get();

            // Check if user has TESTER, QA, or BA role
            boolean hasValidRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("TESTER") || role.getName().equals("QA") || role.getName().equals("BA"));

            if (!hasValidRole) {
                throw new RuntimeException("User must have TESTER, QA, or BA role to be assigned to test modules");
            }

            // Check if user belongs to same organization as current user
            String currentUserOrganization = getCurrentUser().getOrganization();
            if (currentUserOrganization == null) {
                currentUserOrganization = "default";
            }
            String userOrganization = user.getOrganization();
            if (userOrganization == null) {
                userOrganization = "default";
            }
            if (!userOrganization.equals(currentUserOrganization)) {
                throw new RuntimeException("User must belong to the same organization as the assigner");
            }

            // Add module to user's assigned test modules if not already assigned
            if (!user.getAssignedTestModules().contains(module)) {
                user.getAssignedTestModules().add(module);
                User savedUser = userRepository.save(user);
                entityManager.flush();

                // Create test executions for all test cases in this module
                createTestExecutionsForModuleAndUser(module, user);

                return savedUser;
            } else {
                return user; // Already assigned
            }
        }
        throw new RuntimeException("User or test module not found with id: " + request.getUserId() + " or " + request.getTestModuleId());
    }

    /**
     * Create test executions for all test cases in a module and assign them to a user
     * Skips test cases that already have executions assigned to this user
     * @param module The test module
     * @param user The user to assign executions to
     */
    private void createTestExecutionsForModuleAndUser(TestModule module, User user) {
        // Fetch all test suites with their test cases for this module in a single query
        List<TestSuite> suites = testSuiteRepository.findByTestModuleIdWithTestCases(module.getId());
        if (suites == null || suites.isEmpty()) {
            System.out.println("No test suites found for module " + module.getId());
            return;
        }
        System.out.println("Creating test executions for module " + module.getId() + " and user " + user.getId() + ", found " + suites.size() + " suites");

        // Get existing executions for this user to avoid duplicates
        List<TestExecution> existingExecutions = testExecutionRepository.findByAssignedToUser(user);

        // Iterate through all test suites and their test cases
        for (TestSuite suite : suites) {
            List<TestCase> testCases = suite.getTestCases();
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
                        System.out.println("Creating execution for test case " + testCase.getId() + " (" + testCase.getTestCaseId() + ")");
                        createTestExecutionForTestCaseAndUser(testCase.getId(), user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other test cases
                        System.err.println("Error creating execution for test case " + testCase.getId() + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("Execution already exists for test case " + testCase.getId() + " and user " + user.getId());
                }
            }
        }
    }

    /**
     * Create a test execution for a specific test case and assign it to a user
     * @param testCaseId The test case ID
     * @param userId The user ID to assign the execution to
     * @return The created test execution
     */
    @Transactional
    public TestExecution createTestExecutionForTestCaseAndUser(Long testCaseId, Long userId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (testCaseOpt.isPresent() && userOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            User user = userOpt.get();

            // Create new test execution
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);
            execution.setExecutionDate(LocalDateTime.now());
            execution.setOverallResult("PENDING");
            execution.setAssignedToUser(user);

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

            return testExecutionRepository.save(execution);
        }
        throw new RuntimeException("Test case or user not found with id: " + testCaseId + " or " + userId);
    }

    /**
     * Remove a user from a test module assignment
     * @param request Module assignment request containing userId and testModuleId
     * @return The updated user
     */
    @Transactional
    public User removeUserFromTestModule(ModuleAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<TestModule> moduleOpt = testModuleRepository.findById(request.getTestModuleId());

        if (userOpt.isPresent() && moduleOpt.isPresent()) {
            User user = userOpt.get();
            TestModule module = moduleOpt.get();

            // Remove module from user's assigned test modules
            if (user.getAssignedTestModules().contains(module)) {
                user.getAssignedTestModules().remove(module);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Not assigned
            }
        }
        throw new RuntimeException("User or test module not found with id: " + request.getUserId() + " or " + request.getTestModuleId());
    }

    /**
     * Get all test modules assigned to the current authenticated user
     * @return List of assigned test modules
     */
    public List<TestModule> getTestModulesAssignedToCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            List<TestModule> modules;
            // If user is ADMIN, return all modules
            if (isAdmin(user)) {
                modules = testModuleRepository.findAll();
            } else {
                // Otherwise return modules where user is assigned OR user is assigned to project
                modules = testModuleRepository.findTestModulesAssignedToUser(user.getId());
            }

            // Force initialization of test cases for counts
            for (TestModule module : modules) {
                if (module.getTestSuites() != null) {
                    module.getTestSuites().size(); // Ensure suites are loaded
                    for (TestSuite suite : module.getTestSuites()) {
                        if (suite.getTestCases() != null) {
                            suite.getTestCases().size(); // Ensure cases are loaded
                        }
                    }
                }
            }
            
            return modules;
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Regenerate test executions for all test cases in a module for all assigned users
     * Useful for generating executions after module assignment
     * @param moduleId ID of the test module
     */
    @Transactional
    public void regenerateExecutionsForModule(Long moduleId) {
        Optional<TestModule> moduleOpt = testModuleRepository.findById(moduleId);
        if (!moduleOpt.isPresent()) {
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
}

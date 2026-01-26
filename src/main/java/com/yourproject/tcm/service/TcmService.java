package com.yourproject.tcm.service;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.CompletionSummaryDTO;
import com.yourproject.tcm.model.dto.ModuleAssignmentRequest;
import com.yourproject.tcm.model.dto.ProjectAssignmentRequest;
import com.yourproject.tcm.model.dto.TestAnalyticsDTO;
import com.yourproject.tcm.model.dto.TestExecutionDTO;
import com.yourproject.tcm.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

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

    public Organization getCurrentUserOrganizationObject() {
        return getCurrentUser().getOrganization();
    }

    public String getCurrentUserOrganization() {
        Organization org = getCurrentUser().getOrganization();
        return org != null ? org.getName() : "default";
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
     * For ADMIN users: returns all projects for their organization
     * For QA/BA/TESTER users: returns projects they have ANY access to (Direct or via Module)
     * @return List of projects based on user role and assignments
     */
    public List<Project> getAllProjects() {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) {
            Organization org = currentUser.getOrganization();
            if (org == null) {
                return new ArrayList<>(); // Should not happen for valid users, but safety first
            }
            return projectRepository.findAllByOrganization(org);
        } else {
            return projectRepository.findProjectsAssignedToUser(currentUser.getId());
        }
    }

    /**
     * Create a new project
     * @param project The project to create
     * @return The created project
     * @throws RuntimeException if a project with the same name already exists in the organization
     */
    @Transactional
    public Project createProject(Project project) {
        User currentUser = getCurrentUser();
        Organization currentOrg = currentUser.getOrganization();

        if (currentOrg == null) {
            throw new RuntimeException("Current user is not assigned to an organization. Please contact support.");
        }

        // Check if a project with the same name already exists in this organization
        Optional<Project> existingProject = projectRepository.findByNameAndOrganization(project.getName(), currentOrg);
        if (existingProject.isPresent()) {
            throw new RuntimeException("A project with name '" + project.getName() + "' already exists");
        }
        
        project.setOrganization(currentOrg);
        return projectRepository.save(project);
    }

    /**
     * Get a specific project by ID
     * For ADMIN users: returns project if it exists and belongs to their org
     * For QA/BA users: returns project only if assigned to them (and implicit org check)
     * @param projectId The ID of the project to retrieve
     * @return Optional containing the project, or empty if not found/not assigned/wrong org
     */
    @Transactional(readOnly = true)  // Read-only transaction for better performance
    public Optional<Project> getProjectById(Long projectId) {
        User currentUser = getCurrentUser();
        Optional<Project> projectOpt = projectRepository.findProjectWithModulesById(projectId);
        
        if (!projectOpt.isPresent()) {
            return Optional.empty();
        }

        Project project = projectOpt.get();
        
        // Security Check: Ensure project belongs to user's organization
        // (Using IDs to be safe against proxy objects or detached entities)
        if (project.getOrganization() == null || currentUser.getOrganization() == null ||
            !project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
             return Optional.empty(); // Treat as not found if it belongs to another org
        }

        // Force initialization of lazy collections
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

            // 1. Clear assignments: Remove this project from all users' assigned lists
            // This is critical for ManyToMany cleanup in the junction table 'user_projects'
            if (project.getAssignedUsers() != null) {
                // Create a copy to avoid concurrent modification exception
                Set<User> users = new HashSet<>(project.getAssignedUsers());
                for (User user : users) {
                    user.getAssignedProjects().remove(project);
                    userRepository.save(user); // Save user to update junction table
                }
                project.getAssignedUsers().clear();
            }
            entityManager.flush(); // Ensure junction table records are gone

            // Delete all test modules associated with this project
            // We must manually delete each module to trigger the detailed cleanup logic in deleteTestModule
            // Just letting CascadeType.ALL handle it might skip the manual cleanup we added there
            if (project.getModules() != null) {
                // Create a copy of the list to iterate over safely while deleting
                List<TestModule> modulesToDelete = new ArrayList<>(project.getModules());
                for (TestModule module : modulesToDelete) {
                    deleteTestModule(module.getId());  // Recursively delete module and its contents
                }
                // Clear the list in the project entity to prevent JPA trying to delete them again
                project.getModules().clear();
            }
            
            entityManager.flush(); // Ensure modules are gone

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
            // Also sort test cases within each suite by ID
            if (testModule.getTestSuites() != null) {
                for (TestSuite testSuite : testModule.getTestSuites()) {
                    TestSuite suiteWithTestCases = suiteMap.get(testSuite.getId());
                    if (suiteWithTestCases != null) {
                        // Sort test cases by ID within the suite
                        if (suiteWithTestCases.getTestCases() != null) {
                            suiteWithTestCases.getTestCases().sort(Comparator.comparing(TestCase::getId));
                        }
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
        // Fetch the module with test suites (using the repository method that fetches suites)
        // We need the suites collection to be initialized to clear it later
        TestModule testModule = testModuleRepository.findByIdWithTestSuites(testModuleId)
                .orElseThrow(() -> new RuntimeException("Test Module not found with id: " + testModuleId));

        // 1. Clear assignments: Remove this module from all users' assigned lists
        if (testModule.getAssignedUsers() != null) {
            Set<User> users = new HashSet<>(testModule.getAssignedUsers());
            for (User user : users) {
                user.getAssignedTestModules().remove(testModule);
                userRepository.save(user); 
            }
            testModule.getAssignedUsers().clear();
        }
        entityManager.flush(); 

        // 2. Clean up test suites deeply
        // We iterate through a copy to perform deep cleanup (executions/results) via deleteTestSuite logic
        // But we DO NOT delete the suite entity itself in the loop, we let orphanRemoval handle it
        if (testModule.getTestSuites() != null) {
            List<TestSuite> suites = new ArrayList<>(testModule.getTestSuites());
            for (TestSuite suite : suites) {
                // Call deleteTestSuite to clean up its children (cases/executions)
                // But we must catch the delete call to avoid double deletion or just extract the cleanup logic
                // Actually, since deleteTestSuite calls repository.delete(), we should just call it
                // and NOT rely on orphanRemoval for the suites themselves, as that's safer for deep structures
                deleteTestSuite(suite.getId());
            }
            
            // Clear the collection to sync the entity state, though they are already deleted
            testModule.getTestSuites().clear();
        }
        
        entityManager.flush();

        // Now delete the module structure
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

            // First, cleanup all execution data for test cases in the suite
            if (testSuite.getTestCases() != null) {
                // We don't delete the test case entity here directly
                // We just clean up the 'grandchildren' (Executions/Results)
                // The test cases themselves will be deleted via orphanRemoval when we clear the list
                for (TestCase testCase : testSuite.getTestCases()) {
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
                testSuite.getTestCases().clear();
                entityManager.flush(); // Force the deletion of test cases
            }

            // Now delete the test suite
            testSuiteRepository.delete(testSuite);
            entityManager.flush(); // Ensure data is written to DB
        } else {
            throw new RuntimeException("Test Suite not found with id: " + suiteId);
        }
    }

    // ==================== TEST CASE METHODS ====================

    /**
     * Get all test cases in the system (filtered by organization)
     * @return List of all test cases for the user's organization
     */
    @Transactional(readOnly = true)
    public List<TestCase> getAllTestCases() {
        User currentUser = getCurrentUser();
        Organization org = currentUser.getOrganization();
        if (org == null) return new ArrayList<>();
        return testCaseRepository.findAllWithDetailsByOrganizationId(org.getId());
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
        if (currentUser == null || currentUser.getOrganization() == null) {
            // No authenticated user or org - return empty analytics
            return new TestAnalyticsDTO(0, 0, 0, 0, 0, 0.0, 0.0, new ArrayList<>(), new ArrayList<>());
        } else if (isAdmin(currentUser)) {
            // Admin can filter by userId or see all
            filterUserId = userId;
        } else {
            // Non-admin users only see their own executions
            filterUserId = currentUser.getId();
        }

        Long orgId = currentUser.getOrganization().getId();

        // Get all test cases with their test suites and modules (eagerly fetched) - Filtered by Organization
        List<TestCase> allTestCases = testCaseRepository.findAllWithDetailsByOrganizationId(orgId);

        // Get all executions with details - Filtered by Organization
        List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);

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
                // Admin filtering by specific user - show all test cases assigned to that user
                // (not just executed ones - includes pending executions)
                Set<Long> userTestCaseIds = allExecutions.stream()
                    .map(ex -> ex.getTestCase().getId())
                    .collect(Collectors.toSet());
                
                filteredTestCases = allTestCases.stream()
                    .filter(tc -> userTestCaseIds.contains(tc.getId()))
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
            if ("PASSED".equalsIgnoreCase(result)) {
                passedCount++;
            } else if ("FAILED".equalsIgnoreCase(result)) {
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
                if ("PASSED".equalsIgnoreCase(result)) {
                    pStats.setPassedCount(pStats.getPassedCount() + 1);
                } else if ("FAILED".equalsIgnoreCase(result)) {
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
                if ("PASSED".equalsIgnoreCase(result2)) {
                    mStats.setPassedCount(mStats.getPassedCount() + 1);
                } else if ("FAILED".equalsIgnoreCase(result2)) {
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

            // Auto-generate executions for all users assigned to the module
            TestModule module = testSuite.getTestModule();
            if (module != null && module.getAssignedUsers() != null && !module.getAssignedUsers().isEmpty()) {
                for (User user : module.getAssignedUsers()) {
                    try {
                        createTestExecutionForTestCaseAndUser(savedTestCase.getId(), user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other users
                        System.err.println("Error creating execution for test case " + savedTestCase.getId() + " and user " + user.getId() + ": " + e.getMessage());
                    }
                }
            }

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
    public List<TestExecutionDTO> getTestExecutionsByTestCaseId(Long testCaseId) {
        List<TestExecution> executions = testExecutionRepository.findByTestCase_Id(testCaseId);
        return executions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
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
            Organization currentUserOrg = getCurrentUser().getOrganization();
            Organization userOrg = user.getOrganization();
            
            // If either organization is null (shouldn't happen in valid setup) or they don't match
            if (currentUserOrg == null || userOrg == null || !currentUserOrg.getId().equals(userOrg.getId())) {
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
    public List<TestExecutionDTO> getTestExecutionsAssignedToUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<TestExecution> executions = testExecutionRepository.findByAssignedToUserWithDetails(user);
            return executions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }
        throw new RuntimeException("User not found with id: " + userId);
    }

    /**
     * Get all test executions assigned to the current authenticated user
     * @return List of assigned test executions as DTOs
     */
    public List<TestExecutionDTO> getTestExecutionsForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            // If user is ADMIN, return all executions but only one per test case (latest)
            if (isAdmin(user)) {
                // Ensure we only get executions for the user's organization
                Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : -1L;
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

                        // Compare by Suite ID
                        int suiteCompare = Long.compare(e1.getTestSuiteId(), e2.getTestSuiteId());
                        if (suiteCompare != 0) return suiteCompare;

                        // Compare by Test Case ID (use testCase's ID)
                        Long tcId1 = e1.getTestCase().getId();
                        Long tcId2 = e2.getTestCase().getId();
                        return Long.compare(tcId1, tcId2);
                    })
                    .map(this::convertToDTO)
                    .collect(java.util.stream.Collectors.toList());
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
                // Add hierarchical sorting: Module ID → Suite ID → Test Case ID
                .sorted((e1, e2) -> {
                    // Compare by Module ID
                    int moduleCompare = Long.compare(e1.getModuleId(), e2.getModuleId());
                    if (moduleCompare != 0) return moduleCompare;

                    // Compare by Suite ID
                    int suiteCompare = Long.compare(e1.getTestSuiteId(), e2.getTestSuiteId());
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
                .collect(java.util.stream.Collectors.toList());
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Get completion summary statistics for the current user
     * @return CompletionSummaryDTO containing total, passed, failed, blocked, and pending counts
     */
    public CompletionSummaryDTO getCompletionSummaryForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            List<TestExecution> executions = testExecutionRepository.findByAssignedToUserWithDetails(user);
            Set<Long> assignedModuleIds = user.getAssignedTestModules().stream()
                .map(TestModule::getId)
                .collect(java.util.stream.Collectors.toSet());

            List<TestExecution> filteredExecutions = executions.stream()
                .filter(e -> {
                    Long moduleId = e.getModuleId();
                    return moduleId != null && assignedModuleIds.contains(moduleId);
                })
                .collect(java.util.stream.Collectors.toList());

            // Calculate statistics
            long total = filteredExecutions.size();
            long passed = filteredExecutions.stream().filter(e -> "PASSED".equals(e.getOverallResult())).count();
            long failed = filteredExecutions.stream().filter(e -> "FAILED".equals(e.getOverallResult())).count();
            long blocked = filteredExecutions.stream().filter(e -> "BLOCKED".equals(e.getOverallResult())).count();
            long pending = total - passed - failed - blocked;

            return new CompletionSummaryDTO(total, passed, failed, blocked, pending);
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Convert TestExecution entity to TestExecutionDTO
     * @param execution The TestExecution entity to convert
     * @return TestExecutionDTO with flattened hierarchy data
     */
    private TestExecutionDTO convertToDTO(TestExecution execution) {
        Long assignedToUserId = null;
        String assignedToUsername = null;
        if (execution.getAssignedToUser() != null) {
            assignedToUserId = execution.getAssignedToUser().getId();
            assignedToUsername = execution.getAssignedToUser().getUsername();
        }
        
        return new TestExecutionDTO(
            execution.getId(),
            execution.getTestCaseId(),
            execution.getTitle(),
            execution.getExecutionDate(),
            execution.getOverallResult(),
            execution.getNotes(),
            execution.getDuration(),
            execution.getEnvironment(),
            execution.getExecutedBy(),
            assignedToUserId,
            assignedToUsername,
            execution.getTestSuiteId(),
            execution.getTestSuiteName(),
            execution.getModuleId(),
            execution.getModuleName(),
            execution.getProjectId(),
            execution.getProjectName(),
            null // stepResults - not needed for this conversion
        );
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
            Organization currentUserOrg = getCurrentUser().getOrganization();
            Organization userOrg = user.getOrganization();
            
            // If either organization is null (shouldn't happen in valid setup) or they don't match
            if (currentUserOrg == null || userOrg == null || !currentUserOrg.getId().equals(userOrg.getId())) {
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
            Organization currentUserOrg = getCurrentUser().getOrganization();
            Organization userOrg = user.getOrganization();
            
            // If either organization is null (shouldn't happen in valid setup) or they don't match
            if (currentUserOrg == null || userOrg == null || !currentUserOrg.getId().equals(userOrg.getId())) {
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
            return;
        }

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
                        createTestExecutionForTestCaseAndUser(testCase.getId(), user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other test cases
                        System.err.println("Error creating execution for test case " + testCase.getId() + ": " + e.getMessage());
                    }
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
            // If user is ADMIN, return all modules in their organization
            if (isAdmin(user)) {
                Organization org = user.getOrganization();
                if (org == null) {
                    throw new RuntimeException("User does not belong to any organization");
                }
                modules = testModuleRepository.findAll().stream()
                    .filter(module -> module.getProject() != null && module.getProject().getOrganization() != null)
                    .filter(module -> module.getProject().getOrganization().getId().equals(org.getId()))
                    .collect(java.util.stream.Collectors.toList());
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
     * Get all non-admin users in the current user's organization
     * Used for admin filtering on execution page
     * @return List of non-admin users (QA, BA, TESTER)
     */
    public List<User> getUsersInOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            // Only admin users can access this
            if (!isAdmin(currentUser)) {
                throw new RuntimeException("Only admin users can access organization users");
            }

            Organization org = currentUser.getOrganization();
            if (org == null) {
                throw new RuntimeException("User does not belong to any organization");
            }

            return userRepository.findAllNonAdminUsers(org);
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Get all modules in the current user's organization
     * Used for admin filtering on execution page
     * @return List of all modules in the organization
     */
    public List<TestModule> getAllModulesInOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            // Only admin users can access this
            if (!isAdmin(currentUser)) {
                throw new RuntimeException("Only admin users can access all organization modules");
            }

            Organization org = currentUser.getOrganization();
            if (org == null) {
                throw new RuntimeException("User does not belong to any organization");
            }

            // Get all modules in the organization
            return testModuleRepository.findAll().stream()
                .filter(module -> module.getProject() != null && module.getProject().getOrganization() != null)
                .filter(module -> module.getProject().getOrganization().getId().equals(org.getId()))
                .collect(java.util.stream.Collectors.toList());
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Get all test executions in the current user's organization
     * Used for admin filtering on execution page - returns all executions (not just latest per test case)
     * This allows admins to filter by assigned user and see all executions assigned to that user
     * @param userId Optional user ID to filter by - when provided, only shows executions from modules the user is currently assigned to
     * @return List of all executions in the organization as DTOs
     */
    public List<TestExecutionDTO> getAllExecutionsInOrganization(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));

            // Only admin users can access this
            if (!isAdmin(currentUser)) {
                throw new RuntimeException("Only admin users can access all organization executions");
            }

            Organization org = currentUser.getOrganization();
            if (org == null) {
                throw new RuntimeException("User does not belong to any organization");
            }

            // Get all executions in the organization (not just latest per test case)
            Long orgId = org.getId();
            List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);

            // If userId is provided, filter by that user's current module assignments
            if (userId != null) {
                User filteredUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

                // Get the user's currently assigned module IDs
                Set<Long> assignedModuleIds = filteredUser.getAssignedTestModules().stream()
                    .map(com.yourproject.tcm.model.TestModule::getId)
                    .collect(java.util.stream.Collectors.toSet());

                // Filter executions to only show those from assigned modules
                allExecutions = allExecutions.stream()
                    .filter(execution -> {
                        Long moduleId = execution.getModuleId();
                        return moduleId != null && assignedModuleIds.contains(moduleId);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }

            // Return filtered executions as DTOs with hierarchical sorting
            return allExecutions.stream()
                .sorted((e1, e2) -> {
                    // Compare by Module ID
                    int moduleCompare = Long.compare(e1.getModuleId(), e2.getModuleId());
                    if (moduleCompare != 0) return moduleCompare;

                    // Compare by Suite ID
                    int suiteCompare = Long.compare(e1.getTestSuiteId(), e2.getTestSuiteId());
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
                .collect(java.util.stream.Collectors.toList());
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

    // ==================== IMPORT/EXPORT METHODS ====================

    /**
     * Import test cases and test suites from Excel file
     * @param moduleId ID of the test module to import into
     * @param file Excel file to import (.xlsx format)
     * @return Import result with statistics
     */
    @Transactional
    public Map<String, Object> importTestCasesFromExcel(Long moduleId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int suitesCreated = 0;
        int testCasesCreated = 0;
        int testCasesSkipped = 0;

        try {
            // Validate file format
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty or null");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                throw new RuntimeException("Invalid file format. Only .xlsx files are supported.");
            }

            // Get module
            Optional<TestModule> moduleOpt = testModuleRepository.findById(moduleId);
            if (!moduleOpt.isPresent()) {
                throw new RuntimeException("Test module not found with id: " + moduleId);
            }

            TestModule module = moduleOpt.get();

            // Parse Excel file
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0); // First sheet

            // Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel file has no header row");
            }

            List<String> expectedHeaders = Arrays.asList("Suite Name", "Test Case ID", "Title", "Description", "Step Number", "Action", "Expected Result");
            for (int i = 0; i < expectedHeaders.size(); i++) {
                Cell cell = headerRow.getCell(i);
                String headerValue = cell != null ? cell.getStringCellValue().trim() : "";
                if (!headerValue.equals(expectedHeaders.get(i))) {
                    throw new RuntimeException("Invalid header at column " + (i + 1) + ". Expected: " + expectedHeaders.get(i) + ", Found: " + headerValue);
                }
            }

            // Get existing test case IDs in this module to check for duplicates
            Set<String> existingTestCaseIds = new HashSet<>();
            for (TestSuite suite : module.getTestSuites()) {
                for (TestCase testCase : suite.getTestCases()) {
                    existingTestCaseIds.add(testCase.getTestCaseId());
                }
            }

            // Parse data rows
            Map<String, List<Map<String, Object>>> testCaseDataMap = new LinkedHashMap<>(); // Maintains insertion order
            int rowNum = 1; // Start from row 1 (after header)

            while (true) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    break; // End of data
                }

                try {
                    // Extract data from row
                    String suiteName = getCellValueAsString(row.getCell(0));
                    String testCaseId = getCellValueAsString(row.getCell(1));
                    String title = getCellValueAsString(row.getCell(2));
                    String description = getCellValueAsString(row.getCell(3));
                    String stepNumberStr = getCellValueAsString(row.getCell(4));
                    String action = getCellValueAsString(row.getCell(5));
                    String expectedResult = getCellValueAsString(row.getCell(6));

                    // Validate required fields
                    if (suiteName == null || suiteName.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Suite Name is required");
                        rowNum++;
                        continue;
                    }
                    if (testCaseId == null || testCaseId.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Test Case ID is required");
                        rowNum++;
                        continue;
                    }
                    if (title == null || title.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Title is required");
                        rowNum++;
                        continue;
                    }
                    if (stepNumberStr == null || stepNumberStr.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Step Number is required");
                        rowNum++;
                        continue;
                    }
                    if (action == null || action.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Action is required");
                        rowNum++;
                        continue;
                    }
                    if (expectedResult == null || expectedResult.trim().isEmpty()) {
                        errors.add("Row " + (rowNum + 1) + ": Expected Result is required");
                        rowNum++;
                        continue;
                    }

                    // Parse step number
                    int stepNumber;
                    try {
                        stepNumber = Integer.parseInt(stepNumberStr.trim());
                        if (stepNumber < 1) {
                            errors.add("Row " + (rowNum + 1) + ": Step Number must be positive (found: " + stepNumber + ")");
                            rowNum++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (rowNum + 1) + ": Step Number must be a valid integer (found: " + stepNumberStr + ")");
                        rowNum++;
                        continue;
                    }

                    // Check for duplicate test case ID in module
                    String testCaseKey = testCaseId.trim();
                    if (existingTestCaseIds.contains(testCaseKey)) {
                        testCasesSkipped++;
                        rowNum++;
                        continue; // Skip this row
                    }

                    // Group test case data by test case ID
                    String key = suiteName.trim() + "|" + testCaseKey;
                    if (!testCaseDataMap.containsKey(key)) {
                        testCaseDataMap.put(key, new ArrayList<>());
                    }

                    Map<String, Object> stepData = new HashMap<>();
                    stepData.put("suiteName", suiteName.trim());
                    stepData.put("testCaseId", testCaseKey);
                    stepData.put("title", title.trim());
                    stepData.put("description", description != null ? description.trim() : "");
                    stepData.put("stepNumber", stepNumber);
                    stepData.put("action", action.trim());
                    stepData.put("expectedResult", expectedResult.trim());

                    testCaseDataMap.get(key).add(stepData);

                    rowNum++;
                } catch (Exception e) {
                    errors.add("Row " + (rowNum + 1) + ": " + e.getMessage());
                    rowNum++;
                }
            }

            workbook.close();

            // If there are validation errors, throw exception to rollback
            if (!errors.isEmpty()) {
                throw new RuntimeException("Validation failed: " + String.join("; ", errors));
            }

            // Create test suites and test cases
            Map<String, TestSuite> suiteMap = new HashMap<>();
            for (String key : testCaseDataMap.keySet()) {
                List<Map<String, Object>> steps = testCaseDataMap.get(key);
                if (steps.isEmpty()) {
                    continue;
                }

                String suiteName = steps.get(0).get("suiteName").toString();
                String testCaseId = steps.get(0).get("testCaseId").toString();
                String title = steps.get(0).get("title").toString();
                String description = steps.get(0).get("description").toString();

                // Find or create test suite
                TestSuite suite;
                if (suiteMap.containsKey(suiteName)) {
                    suite = suiteMap.get(suiteName);
                } else {
                    // Check if suite already exists in module
                    suite = module.getTestSuites().stream()
                        .filter(s -> s.getName().equals(suiteName))
                        .findFirst()
                        .orElse(null);

                    if (suite == null) {
                        suite = new TestSuite();
                        suite.setName(suiteName);
                        suite.setTestModule(module);
                        suite = testSuiteRepository.save(suite);
                        suitesCreated++;
                    }
                    suiteMap.put(suiteName, suite);
                }

                // Create test case
                TestCase testCase = new TestCase();
                testCase.setTestCaseId(testCaseId);
                testCase.setTitle(title);
                testCase.setDescription(description);
                testCase.setTestSuite(suite);

                // Create test steps
                List<TestStep> testSteps = new ArrayList<>();
                for (Map<String, Object> stepData : steps) {
                    TestStep step = new TestStep();
                    step.setStepNumber((Integer) stepData.get("stepNumber"));
                    step.setAction(stepData.get("action").toString());
                    step.setExpectedResult(stepData.get("expectedResult").toString());
                    step.setTestCase(testCase);
                    testSteps.add(step);
                }

                // Sort steps by step number
                testSteps.sort((a, b) -> a.getStepNumber().compareTo(b.getStepNumber()));
                testCase.setTestSteps(testSteps);

                testCase = testCaseRepository.save(testCase);
                entityManager.flush(); // Ensure steps are persisted to database
                testCasesCreated++;

                // Auto-generate executions for assigned users
                if (module.getAssignedUsers() != null) {
                    for (User user : module.getAssignedUsers()) {
                        try {
                            createTestExecutionForTestCaseAndUser(testCase.getId(), user.getId());
                        } catch (Exception e) {
                            // Log error but continue
                            System.err.println("Error creating execution for test case " + testCase.getId() + " and user " + user.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Build result
            result.put("success", true);
            result.put("message", "Import completed successfully");
            result.put("suitesCreated", suitesCreated);
            result.put("testCasesCreated", testCasesCreated);
            result.put("testCasesSkipped", testCasesSkipped);
            result.put("errors", errors);

        } catch (RuntimeException e) {
            // Re-throw to trigger rollback
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Download Excel template for test case import
     * @return Excel template file as byte array
     */
    public byte[] downloadExcelTemplate() throws IOException {
        Resource resource = new ClassPathResource("templates/test-case-import-template.xlsx");
        InputStream inputStream = resource.getInputStream();
        return inputStream.readAllBytes();
    }
}

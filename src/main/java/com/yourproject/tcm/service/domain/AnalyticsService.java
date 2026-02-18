package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.CompletionSummaryDTO;
import com.yourproject.tcm.model.dto.TestAnalyticsDTO;
import com.yourproject.tcm.repository.*;
import com.yourproject.tcm.service.SecurityHelper;
import com.yourproject.tcm.service.UserContextService;
import com.yourproject.tcm.repository.TestModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for analytics-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class AnalyticsService {

    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final SecurityHelper securityHelper;
    private final TestModuleRepository testModuleRepository;

    @Autowired
    public AnalyticsService(
            TestCaseRepository testCaseRepository,
            TestExecutionRepository testExecutionRepository,
            UserRepository userRepository,
            UserContextService userContextService,
            SecurityHelper securityHelper,
            TestModuleRepository testModuleRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.securityHelper = securityHelper;
        this.testModuleRepository = testModuleRepository;
    }

    /**
     * Get test execution analytics
     * For ADMIN users: returns analytics for all test cases in their organization
     * For QA/BA/TESTER users: returns analytics only for test cases in modules they're assigned to
     * @param userId Optional user ID to filter analytics for a specific user (admin only)
     * @return TestAnalyticsDTO containing overall KPIs and breakdown by project/module
     */
    @Transactional(readOnly = true)
    public TestAnalyticsDTO getTestAnalytics(Long userId) {
        // Get the current user
        User currentUser = userContextService.getCurrentUser();
        Organization org = currentUser.getOrganization();
        if (org == null) {
            // No organization - return empty analytics
            return new TestAnalyticsDTO(0, 0, 0, 0, 0, 0.0, 0.0, new ArrayList<>(), new ArrayList<>());
        }

        // Determine which user's executions to show
        final Long filterUserId;
        if (securityHelper.canViewAllOrganizationExecutions(currentUser)) {
            // ADMIN/PROJECT_MANAGER can filter by userId or see all
            filterUserId = userId;
        } else {
            // Non-admin/PM users only see their own executions
            filterUserId = currentUser.getId();
        }

        Long orgId = org.getId();

        // Get all test cases with their test suites and modules (eagerly fetched) - Filtered by Organization
        List<TestCase> allTestCases = testCaseRepository.findAllWithDetailsByOrganizationId(orgId);

        // Get all executions with details - Filtered by Organization
        List<TestExecution> allExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);

        // For PROJECT_MANAGER, filter to only their assigned projects (BOTH test cases AND executions)
        Set<Long> viewableProjectIds = securityHelper.getViewableProjectIds(currentUser);
        if (viewableProjectIds != null && !viewableProjectIds.isEmpty()) {
            List<Long> viewableProjectIdList = viewableProjectIds.stream().collect(Collectors.toList());
            List<Long> viewableModuleIds = testModuleRepository.findModuleIdsByProjectIds(viewableProjectIdList);
            Set<Long> viewableModuleIdSet = new HashSet<>(viewableModuleIds);
            
            // Filter test cases to only those in PM's assigned projects
            allTestCases = allTestCases.stream()
                .filter(tc -> {
                    if (tc.getSubmodule() == null || tc.getSubmodule().getTestModule() == null) {
                        return false;
                    }
                    Long moduleId = tc.getSubmodule().getTestModule().getId();
                    return viewableModuleIdSet.contains(moduleId);
                })
                .collect(Collectors.toList());
            
            // Filter executions to only those in PM's assigned projects
            allExecutions = allExecutions.stream()
                .filter(e -> e.getModuleId() != null && viewableModuleIdSet.contains(e.getModuleId()))
                .collect(Collectors.toList());
        }

        // Filter executions by user if filterUserId is set
        if (filterUserId != null) {
            allExecutions = allExecutions.stream()
                .filter(execution -> {
                    User assignedUser = execution.getAssignedToUser();
                    return assignedUser != null && assignedUser.getId().equals(filterUserId);
                })
                .collect(Collectors.toList());

            // For non-admin/PM users, also filter by assigned modules (match execution page logic)
            if (!securityHelper.canViewAllOrganizationExecutions(currentUser)) {
                Set<Long> assignedModuleIds = currentUser.getAssignedModulesForExecution().stream()
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
        if (securityHelper.canViewAllOrganizationExecutions(currentUser)) {
            // Admin/PM seeing all test cases in their org/projects (no user filter)
            if (filterUserId == null) {
                // Admin/PM seeing all test cases (no user filter)
                filteredTestCases = allTestCases;
            } else {
                // Admin/PM filtering by specific user - show all test cases assigned to that user
                // (not just executed ones - includes pending executions)
                Set<Long> userTestCaseIds = allExecutions.stream()
                    .map(ex -> ex.getTestCase().getId())
                    .collect(Collectors.toSet());

                filteredTestCases = allTestCases.stream()
                    .filter(tc -> userTestCaseIds.contains(tc.getId()))
                    .collect(Collectors.toList());
            }
        } else {
            // Non-admin/PM users (QA/BA/TESTER) see ONLY test cases they have executions for
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

        // For admin/PM users, use executedTestCaseIds directly
        // For non-admin users (QA/BA/TESTER), calculate executedCount from filteredTestCases that have executions
        long executedCount;
        if (securityHelper.canViewAllOrganizationExecutions(currentUser)) {
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
            Submodule suite = testCase.getSubmodule();
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
     * Get completion summary statistics for the current user
     * - ADMIN/PROJECT_MANAGER: returns summary for all executions in org/assigned projects
     * - QA/BA/TESTER: returns summary for their assigned modules only
     * @return CompletionSummaryDTO containing total, passed, failed, blocked, and pending counts
     */
    @Transactional(readOnly = true)
    public CompletionSummaryDTO getCompletionSummaryForCurrentUser() {
        User user = userContextService.getCurrentUser();
        
        List<TestExecution> executions;
        
        // ADMIN/PROJECT_MANAGER see all executions in org/assigned projects
        if (securityHelper.canViewAllOrganizationExecutions(user)) {
            Set<Long> viewableProjectIds = securityHelper.getViewableProjectIds(user);
            Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : -1L;
            
            List<TestExecution> allOrgExecutions = testExecutionRepository.findAllWithDetailsByOrganizationId(orgId);
            
            if (viewableProjectIds == null) {
                // ADMIN sees all
                executions = allOrgExecutions;
            } else {
                // PROJECT_MANAGER sees only their assigned projects
                List<Long> viewableProjectIdList = new ArrayList<>(viewableProjectIds);
                List<Long> viewableModuleIds = testModuleRepository.findModuleIdsByProjectIds(viewableProjectIdList);
                Set<Long> viewableModuleIdSet = new HashSet<>(viewableModuleIds);
                
                executions = allOrgExecutions.stream()
                    .filter(e -> e.getModuleId() != null && viewableModuleIdSet.contains(e.getModuleId()))
                    .collect(Collectors.toList());
            }
        } else {
            // QA/BA/TESTER see only their assigned modules
            executions = testExecutionRepository.findByAssignedToUserWithDetails(user);
            Set<Long> assignedModuleIds = user.getAssignedModulesForExecution().stream()
                .map(TestModule::getId)
                .collect(Collectors.toSet());

            executions = executions.stream()
                .filter(e -> {
                    Long moduleId = e.getModuleId();
                    return moduleId != null && assignedModuleIds.contains(moduleId);
                })
                .collect(Collectors.toList());
        }

        // Calculate statistics
        long total = executions.size();
        long passed = executions.stream().filter(e -> "PASSED".equals(e.getOverallResult())).count();
        long failed = executions.stream().filter(e -> "FAILED".equals(e.getOverallResult())).count();
        long blocked = executions.stream().filter(e -> "BLOCKED".equals(e.getOverallResult())).count();
        long pending = total - passed - failed - blocked;

        return new CompletionSummaryDTO(total, passed, failed, blocked, pending);
    }
}
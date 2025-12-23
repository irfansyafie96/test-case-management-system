package com.yourproject.tcm.controller;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.StepResultResponse;
import com.yourproject.tcm.service.TcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * API Controller - Main REST API Endpoint for the Test Case Management System
 *
 * This controller handles all HTTP requests starting with "/api" and delegates
 * business logic to TcmService. It provides endpoints for managing the entire
 * test case hierarchy: Projects → Modules → Suites → Test Cases → Test Steps
 * and their executions/results.
 *
 * Security: Uses @PreAuthorize annotations to ensure proper role-based access control
 * - ADMIN: Full access, can delete anything
 * - QA/BA: Can create and update, but not delete
 * - TESTER: Read-only access for executing tests
 *
 * HTTP Methods Used:
 * - GET: Retrieve data
 * - POST: Create new entities
 * - PUT: Update existing entities
 * - DELETE: Remove entities
 */
@RestController
@RequestMapping("/api")  // All endpoints in this controller start with /api
public class ApiController {

    private final TcmService tcmService;

    @Autowired
    public ApiController(TcmService tcmService) {
        this.tcmService = tcmService;
    }

    // ==================== PROJECT ENDPOINTS ====================

    /**
     * GET /api/projects - Get all projects
     * @return ResponseEntity with list of all projects and HTTP 200 OK
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = tcmService.getAllProjects();
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

    /**
     * POST /api/projects - Create a new project
     * Requires ADMIN, QA, or BA role
     * @param project Project data from request body
     * @return ResponseEntity with created project and HTTP 201 CREATED
     * @throws RuntimeException if project creation fails (e.g., duplicate name)
     *         Handled by GlobalExceptionHandler with appropriate HTTP status codes
     */
    @PostMapping("/projects")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")  // Role-based access
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project savedProject = tcmService.createProject(project);
        return new ResponseEntity<>(savedProject, HttpStatus.CREATED);
    }

    /**
     * GET /api/projects/{projectId} - Get a specific project by ID
     * @param projectId ID of the project to retrieve
     * @return ResponseEntity with project data or error message
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<?> getProjectById(@PathVariable Long projectId) {
        try {
            Optional<Project> projectOpt = tcmService.getProjectById(projectId);
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                // Only return basic project info to avoid circular references
                return new ResponseEntity<>(project, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Project not found with id: " + projectId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/projects/{projectId} - Delete a project and all its contents
     * Requires ADMIN role only
     * @param projectId ID of the project to delete
     * @return ResponseEntity with 204 No Content on success
     * @throws RuntimeException if project not found or deletion fails
     *         Handled by GlobalExceptionHandler with appropriate HTTP status codes
     */
    @DeleteMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")  // Only ADMIN can delete projects
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        // First check if project exists
        Optional<Project> projectOpt = tcmService.getProjectById(projectId);
        if (!projectOpt.isPresent()) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }

        // Use tcmService to delete the project (which handles cascading deletions)
        tcmService.deleteProject(projectId);
        return ResponseEntity.noContent().build(); // Return 204 No Content for successful delete
    }

    // ==================== MODULE ENDPOINTS ====================

    /**
     * POST /api/projects/{projectId}/testmodules - Create a test module for a project
     * Requires ADMIN, QA, or BA role
     * @param projectId Parent project ID
     * @param testModule Module data from request body
     * @return ResponseEntity with created module and HTTP 201 CREATED
     * @throws RuntimeException if parent project not found or other validation fails
     *         Handled by GlobalExceptionHandler with appropriate HTTP status codes
     */
    @PostMapping("/projects/{projectId}/testmodules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<TestModule> createTestModuleForProject(@PathVariable Long projectId, @RequestBody TestModule testModule) {
        TestModule savedTestModule = tcmService.createTestModuleForProject(projectId, testModule);
        return new ResponseEntity<>(savedTestModule, HttpStatus.CREATED);
    }

    /**
     * GET /api/testmodules/{testModuleId} - Get a specific test module by ID with its suites and test cases
     * @param testModuleId ID of the test module to retrieve
     * @return ResponseEntity with module data or error
     */
    @GetMapping("/testmodules/{testModuleId}")
    public ResponseEntity<?> getTestModuleById(@PathVariable Long testModuleId) {
        try {
            Optional<TestModule> testModuleOpt = tcmService.getTestModuleById(testModuleId);
            if (testModuleOpt.isPresent()) {
                return new ResponseEntity<>(testModuleOpt.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Test Module not found with id: " + testModuleId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/testmodules/{testModuleId} - Update a test module (name only)
     * Requires ADMIN, QA, or BA role
     * @param testModuleId ID of the module to update
     * @param testModuleDetails Updated module data
     * @return ResponseEntity with updated module or error
     */
    @PutMapping("/testmodules/{testModuleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestModule(@PathVariable Long testModuleId, @RequestBody TestModule testModuleDetails) {
        try {
            TestModule updatedTestModule = tcmService.updateTestModule(testModuleId, testModuleDetails);
            return new ResponseEntity<>(updatedTestModule, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/testmodules/{testModuleId} - Delete a test module and all its contents
     * Requires ADMIN role only
     * @param testModuleId ID of the module to delete
     * @return ResponseEntity with appropriate status
     */
    @DeleteMapping("/testmodules/{testModuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTestModule(@PathVariable Long testModuleId) {
        try {
            tcmService.deleteTestModule(testModuleId);
            return ResponseEntity.noContent().build(); // Return 204 No Content for successful delete
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting test module: " + e.getMessage());
        }
    }

    // ==================== TEST SUITE ENDPOINTS ====================

    /**
     * POST /api/testmodules/{testModuleId}/testsuites - Create a test suite for a module
     * Requires ADMIN, QA, or BA role
     * @param testModuleId Parent module ID
     * @param testSuite Suite data from request body
     * @return ResponseEntity with created suite or error
     */
    @PostMapping("/testmodules/{testModuleId}/testsuites")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> createTestSuiteForTestModule(@PathVariable Long testModuleId, @RequestBody TestSuite testSuite) {
        try {
            TestSuite savedTestSuite = tcmService.createTestSuiteForTestModule(testModuleId, testSuite);
            return new ResponseEntity<>(savedTestSuite, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/testsuites/{suiteId} - Get a specific test suite by ID with its test cases
     * @param suiteId ID of the test suite to retrieve
     * @return ResponseEntity with suite data or error
     */
    @GetMapping("/testsuites/{suiteId}")
    public ResponseEntity<?> getTestSuiteById(@PathVariable Long suiteId) {
        try {
            Optional<TestSuite> testSuiteOpt = tcmService.getTestSuiteById(suiteId);
            if (testSuiteOpt.isPresent()) {
                return new ResponseEntity<>(testSuiteOpt.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Test suite not found with id: " + suiteId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/testsuites/{suiteId} - Update a test suite (name only)
     * Requires ADMIN, QA, or BA role
     * @param suiteId ID of the suite to update
     * @param suiteDetails Updated suite data
     * @return ResponseEntity with updated suite or error
     */
    @PutMapping("/testsuites/{suiteId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestSuite(@PathVariable Long suiteId, @RequestBody TestSuite suiteDetails) {
        try {
            TestSuite updatedTestSuite = tcmService.updateTestSuite(suiteId, suiteDetails);
            return new ResponseEntity<>(updatedTestSuite, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== TEST CASE ENDPOINTS ====================

    /**
     * POST /api/testsuites/{suiteId}/testcases - Create a test case for a suite
     * Requires ADMIN, QA, or BA role
     * Note: Request body should include the list of TestSteps
     * @param suiteId Parent suite ID
     * @param testCase Test case data (including steps) from request body
     * @return ResponseEntity with created test case and HTTP 201 CREATED
     * @throws RuntimeException if parent suite not found or other validation fails
     *         Handled by GlobalExceptionHandler with appropriate HTTP status codes
     */
    @PostMapping("/testsuites/{suiteId}/testcases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<TestCase> createTestCaseForTestSuite(@PathVariable Long suiteId, @RequestBody TestCase testCase) {
        TestCase savedTestCase = tcmService.createTestCaseForTestSuite(suiteId, testCase);
        return new ResponseEntity<>(savedTestCase, HttpStatus.CREATED);
    }

    /**
     * GET /api/testcases - Get all test cases in the system
     * @return ResponseEntity with list of all test cases or error
     */
    @GetMapping("/testcases")
    public ResponseEntity<List<TestCase>> getAllTestCases() {
        try {
            List<TestCase> testCases = tcmService.getAllTestCases();
            return new ResponseEntity<>(testCases, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/testcases/{testCaseId} - Get a specific test case by ID with its steps
     * @param testCaseId ID of the test case to retrieve
     * @return ResponseEntity with test case data or error
     */
    @GetMapping("/testcases/{testCaseId}")
    public ResponseEntity<?> getTestCaseById(@PathVariable Long testCaseId) {
        try {
            Optional<TestCase> testCaseOpt = tcmService.getTestCaseById(testCaseId);
            if (testCaseOpt.isPresent()) {
                return new ResponseEntity<>(testCaseOpt.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Test case not found with id: " + testCaseId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/testcases/{testCaseId} - Update a test case and its steps
     * Requires ADMIN, QA, or BA role
     * @param testCaseId ID of the test case to update
     * @param testCaseDetails Updated test case data (including steps)
     * @return ResponseEntity with updated test case or error
     */
    @PutMapping("/testcases/{testCaseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestCase(@PathVariable Long testCaseId, @RequestBody TestCase testCaseDetails) {
        try {
            TestCase updatedTestCase = tcmService.updateTestCase(testCaseId, testCaseDetails);
            return new ResponseEntity<>(updatedTestCase, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/testcases/{testCaseId} - Delete a test case and its executions
     * Requires ADMIN role only
     * @param testCaseId ID of the test case to delete
     * @return ResponseEntity with success message or error
     */
    @DeleteMapping("/testcases/{testCaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTestCase(@PathVariable Long testCaseId) {
        try {
            tcmService.deleteTestCase(testCaseId);
            return new ResponseEntity<>("Test case deleted successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== TEST EXECUTION ENDPOINTS ====================

    /**
     * POST /api/testcases/{testCaseId}/executions - Start a new test execution
     * Creates an execution record and step result records for all steps in the test case
     * @param testCaseId ID of the test case to execute
     * @return ResponseEntity with created execution or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // All roles can create executions
    @PostMapping("/testcases/{testCaseId}/executions")
    public ResponseEntity<?> createTestExecutionForTestCase(@PathVariable Long testCaseId) {
        try {
            TestExecution execution = tcmService.createTestExecutionForTestCase(testCaseId);
            return new ResponseEntity<>(execution, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating test execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/executions/{executionId} - Get a specific test execution by ID with its step results
     * @param executionId ID of the execution to retrieve
     * @return ResponseEntity with execution data or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // All roles can view executions
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<?> getTestExecutionById(@PathVariable Long executionId) {
        try {
            Optional<TestExecution> executionOpt = tcmService.getTestExecutionById(executionId);
            if (executionOpt.isPresent()) {
                return new ResponseEntity<>(executionOpt.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Test execution not found with id: " + executionId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/testcases/{testCaseId}/executions - Get all executions for a specific test case
     * @param testCaseId ID of the test case
     * @return ResponseEntity with list of executions or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // All roles can view executions
    @GetMapping("/testcases/{testCaseId}/executions")
    public ResponseEntity<?> getTestExecutionsByTestCaseId(@PathVariable Long testCaseId) {
        try {
            List<TestExecution> executions = tcmService.getTestExecutionsByTestCaseId(testCaseId);
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/executions/{executionId}/steps/{stepId} - Update the result of a specific step in an execution
     * @param executionId ID of the execution
     * @param stepId ID of the step
     * @param stepData Step result data (status and actual result) from request body
     * @return ResponseEntity with updated step result or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // All roles can update execution steps
    @PutMapping("/executions/{executionId}/steps/{stepId}")
    public ResponseEntity<?> updateStepResult(
            @PathVariable Long executionId,
            @PathVariable Long stepId,
            @Valid @RequestBody StepResultRequest stepData,
            BindingResult bindingResult) {
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return new ResponseEntity<>("Validation errors: " + errors.toString(), HttpStatus.BAD_REQUEST);
        }
        try {
            String status = stepData.getStatus();
            String actualResult = stepData.getActualResult();
            StepResultResponse updatedResult = tcmService.updateStepResult(executionId, stepId, status, actualResult);
            return new ResponseEntity<>(updatedResult, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating step result: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * PUT /api/executions/{executionId}/complete - Complete a test execution
     * @param executionId ID of the execution to complete
     * @param completeData Completion data (overall result and notes) from request body
     * @return ResponseEntity with completed execution or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // All roles can complete executions
    @PutMapping("/executions/{executionId}/complete")
    public ResponseEntity<?> completeTestExecution(
            @PathVariable Long executionId,
            @Valid @RequestBody ExecutionCompleteRequest completeData,
            BindingResult bindingResult) {
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return new ResponseEntity<>("Validation errors: " + errors.toString(), HttpStatus.BAD_REQUEST);
        }
        try {
            String overallResult = completeData.getOverallResult();
            String notes = completeData.getNotes();
            TestExecution completedExecution = tcmService.completeTestExecution(executionId, overallResult, notes);
            return new ResponseEntity<>(completedExecution, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error completing test execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== TEST EXECUTION ASSIGNMENT ENDPOINTS ====================

    /**
     * POST /api/executions/{executionId}/assign - Assign a test execution to a user
     * Assigns a specific test execution to a user for execution
     * @param executionId ID of the execution to assign
     * @param userId ID of the user to assign to
     * @return ResponseEntity with the updated execution or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")  // Managers can assign executions
    @PostMapping("/executions/{executionId}/assign")
    public ResponseEntity<?> assignTestExecution(@PathVariable Long executionId, @RequestParam Long userId) {
        try {
            TestExecution assignedExecution = tcmService.assignTestExecutionToUser(executionId, userId);
            return new ResponseEntity<>(assignedExecution, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error assigning test execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/executions/assigned-to/{userId} - Get all test executions assigned to a specific user
     * @param userId ID of the user
     * @return ResponseEntity with list of assigned executions or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")  // Managers can see assigned executions to any user
    @GetMapping("/executions/assigned-to/{userId}")
    public ResponseEntity<?> getTestExecutionsAssignedToUser(@PathVariable Long userId) {
        try {
            List<TestExecution> executions = tcmService.getTestExecutionsAssignedToUser(userId);
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving assigned test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/executions/my-assignments - Get all test executions assigned to current user
     * @return ResponseEntity with list of assigned executions or error
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")  // Any role can see their own assignments
    @GetMapping("/executions/my-assignments")
    public ResponseEntity<?> getMyAssignedExecutions() {
        try {
            List<TestExecution> executions = tcmService.getTestExecutionsForCurrentUser();
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving your assigned test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

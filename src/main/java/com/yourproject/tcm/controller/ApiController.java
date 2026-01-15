package com.yourproject.tcm.controller;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.*;
import com.yourproject.tcm.repository.UserRepository;
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
import java.util.stream.Collectors;

/**
 * API Controller - Main REST API Endpoint for the Test Case Management System
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final TcmService tcmService;
    private final UserRepository userRepository;

    @Autowired
    public ApiController(TcmService tcmService, UserRepository userRepository) {
        this.tcmService = tcmService;
        this.userRepository = userRepository;
    }

    // ==================== PROJECT ENDPOINTS ====================

    /**
     * GET /api/projects - Get all projects (DTO)
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<Project> projects = tcmService.getAllProjects();
        List<ProjectDTO> projectDTOs = projects.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return new ResponseEntity<>(projectDTOs, HttpStatus.OK);
    }

    /**
     * POST /api/projects - Create a new project (DTO)
     */
    @PostMapping("/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectDTO> createProject(@RequestBody Project project) {
        Project savedProject = tcmService.createProject(project);
        return new ResponseEntity<>(convertToDTO(savedProject), HttpStatus.CREATED);
    }

    /**
     * Helper to map Entity to DTO
     */
    private ProjectDTO convertToDTO(Project p) {
        return new ProjectDTO(
            p.getId(), 
            p.getName(), 
            p.getDescription(), 
            p.getOrganization() != null ? p.getOrganization().getName() : null,
            p.getCreatedDate(),
            p.getUpdatedDate(),
            p.getCreatedBy()
        );
    }

    private TestModuleDTO convertToDTO(TestModule m) {
        return new TestModuleDTO(
            m.getId(),
            m.getName(),
            m.getDescription(),
            m.getProject() != null ? m.getProject().getId() : null,
            m.getProject() != null ? m.getProject().getName() : null
        );
    }

    private TestSuiteDTO convertToDTO(TestSuite s) {
        return new TestSuiteDTO(
            s.getId(),
            s.getName(),
            s.getTestModule() != null ? s.getTestModule().getId() : null,
            s.getTestModule() != null ? s.getTestModule().getName() : null
        );
    }

    private TestCaseDTO convertToDTO(TestCase c) {
        return new TestCaseDTO(
            c.getId(),
            c.getTestCaseId(),
            c.getTitle(),
            c.getDescription(),
            c.getTestSuite() != null ? c.getTestSuite().getId() : null,
            c.getTestSuiteName(),
            c.getModuleName(),
            c.getProjectName(),
            c.getTestSteps() != null ? c.getTestSteps().size() : 0
        );
    }

    /**
     * GET /api/projects/{projectId} - Get a specific project by ID
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<?> getProjectById(@PathVariable Long projectId) {
        try {
            Optional<Project> projectOpt = tcmService.getProjectById(projectId);
            if (projectOpt.isPresent()) {
                return new ResponseEntity<>(projectOpt.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Project not found with id: " + projectId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/projects/{projectId} - Delete a project and all its contents
     */
    @DeleteMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        Optional<Project> projectOpt = tcmService.getProjectById(projectId);
        if (!projectOpt.isPresent()) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        tcmService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    // ==================== MODULE ENDPOINTS ====================

    @PostMapping("/projects/{projectId}/testmodules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<TestModuleDTO> createTestModuleForProject(@PathVariable Long projectId, @RequestBody TestModule testModule) {
        TestModule savedTestModule = tcmService.createTestModuleForProject(projectId, testModule);
        return new ResponseEntity<>(convertToDTO(savedTestModule), HttpStatus.CREATED);
    }

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

    @PutMapping("/testmodules/{testModuleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestModule(@PathVariable Long testModuleId, @RequestBody TestModule testModuleDetails) {
        try {
            TestModule updatedTestModule = tcmService.updateTestModule(testModuleId, testModuleDetails);
            return new ResponseEntity<>(convertToDTO(updatedTestModule), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/testmodules/{testModuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTestModule(@PathVariable Long testModuleId) {
        try {
            tcmService.deleteTestModule(testModuleId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting test module: " + e.getMessage());
        }
    }

    // ==================== TEST SUITE ENDPOINTS ====================

    @PostMapping("/testmodules/{testModuleId}/testsuites")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> createTestSuiteForTestModule(@PathVariable Long testModuleId, @RequestBody TestSuite testSuite) {
        try {
            TestSuite savedTestSuite = tcmService.createTestSuiteForTestModule(testModuleId, testSuite);
            return new ResponseEntity<>(convertToDTO(savedTestSuite), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

    @PutMapping("/testsuites/{suiteId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestSuite(@PathVariable Long suiteId, @RequestBody TestSuite suiteDetails) {
        try {
            TestSuite updatedTestSuite = tcmService.updateTestSuite(suiteId, suiteDetails);
            return new ResponseEntity<>(convertToDTO(updatedTestSuite), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/testsuites/{suiteId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> deleteTestSuite(@PathVariable Long suiteId) {
        try {
            tcmService.deleteTestSuite(suiteId);
            return new ResponseEntity<>("Test suite deleted successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting test suite: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== TEST CASE ENDPOINTS ====================

    @PostMapping("/testsuites/{suiteId}/testcases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<TestCaseDTO> createTestCaseForTestSuite(@PathVariable Long suiteId, @RequestBody TestCase testCase) {
        TestCase savedTestCase = tcmService.createTestCaseForTestSuite(suiteId, testCase);
        return new ResponseEntity<>(convertToDTO(savedTestCase), HttpStatus.CREATED);
    }

    @GetMapping("/testcases")
    public ResponseEntity<List<TestCaseDTO>> getAllTestCases() {
        try {
            List<TestCase> testCases = tcmService.getAllTestCases();
            List<TestCaseDTO> testCaseDTOs = testCases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return new ResponseEntity<>(testCaseDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/testcases/analytics")
    public ResponseEntity<?> getTestAnalytics(@RequestParam(required = false) Long userId) {
        try {
            TestAnalyticsDTO analytics = tcmService.getTestAnalytics(userId);
            return new ResponseEntity<>(analytics, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving analytics: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/testcases/{testCaseId}")
    public ResponseEntity<?> getTestCaseById(@PathVariable Long testCaseId) {
        try {
            Optional<TestCase> testCaseOpt = tcmService.getTestCaseById(testCaseId);
            if (testCaseOpt.isPresent()) {
                return new ResponseEntity<>(convertToDTO(testCaseOpt.get()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Test case not found with id: " + testCaseId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/testcases/{testCaseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> updateTestCase(@PathVariable Long testCaseId, @RequestBody TestCase testCaseDetails) {
        try {
            TestCase updatedTestCase = tcmService.updateTestCase(testCaseId, testCaseDetails);
            return new ResponseEntity<>(convertToDTO(updatedTestCase), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
    @GetMapping("/testcases/{testCaseId}/executions")
    public ResponseEntity<?> getTestExecutionsByTestCaseId(@PathVariable Long testCaseId) {
        try {
            List<TestExecutionDTO> executions = tcmService.getTestExecutionsByTestCaseId(testCaseId);
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
    @PutMapping("/executions/{executionId}/steps/{stepId}")
    public ResponseEntity<?> updateStepResult(
            @PathVariable Long executionId,
            @PathVariable Long stepId,
            @Valid @RequestBody StepResultRequest stepData,
            BindingResult bindingResult) {
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
    @PutMapping("/executions/{executionId}/complete")
    public ResponseEntity<?> completeTestExecution(
            @PathVariable Long executionId,
            @Valid @RequestBody ExecutionCompleteRequest completeData,
            BindingResult bindingResult) {
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @PostMapping("/executions/{executionId}/assign")
    public ResponseEntity<?> assignTestExecution(@PathVariable Long executionId, @RequestParam Long userId) {
        try {
            TestExecution assignedExecution = tcmService.assignTestExecutionToUser(executionId, userId);
            return new ResponseEntity<>(assignedExecution, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error assigning test execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @GetMapping("/executions/assigned-to/{userId}")
    public ResponseEntity<?> getTestExecutionsAssignedToUser(@PathVariable Long userId) {
        try {
            List<TestExecutionDTO> executions = tcmService.getTestExecutionsAssignedToUser(userId);
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving assigned test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
    @GetMapping("/executions/my-assignments")
    public ResponseEntity<?> getMyAssignedExecutions() {
        try {
            List<TestExecutionDTO> executions = tcmService.getTestExecutionsForCurrentUser();
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving your assigned test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== PROJECT ASSIGNMENT ENDPOINTS ====================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/projects/assign")
    public ResponseEntity<?> assignUserToProject(@Valid @RequestBody ProjectAssignmentRequest request) {
        try {
            User updatedUser = tcmService.assignUserToProject(request);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error assigning user to project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/projects/assign")
    public ResponseEntity<?> removeUserFromProject(@Valid @RequestBody ProjectAssignmentRequest request) {
        try {
            User updatedUser = tcmService.removeUserFromProject(request);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error removing user from project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @GetMapping("/projects/assigned-to-me")
    public ResponseEntity<?> getProjectsAssignedToCurrentUser() {
        try {
            List<Project> projects = tcmService.getProjectsAssignedToCurrentUser();
            return new ResponseEntity<>(projects, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving assigned projects: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/projects/{projectId}/assigned-users")
    public ResponseEntity<?> getUsersAssignedToProject(@PathVariable Long projectId) {
        try {
            List<User> users = userRepository.findUsersAssignedToProject(projectId);
            // Convert to DTOs to avoid serialization issues
            List<com.yourproject.tcm.model.dto.UserDTO> userDTOs = users.stream()
                .map(user -> new com.yourproject.tcm.model.dto.UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getOrganizationName(),
                    user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toList())
                ))
                .collect(java.util.stream.Collectors.toList());
            return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving users assigned to project: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== MODULE ASSIGNMENT ENDPOINTS ====================

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @PostMapping("/testmodules/assign")
    public ResponseEntity<?> assignUserToTestModule(@Valid @RequestBody ModuleAssignmentRequest request) {
        try {
            User updatedUser = tcmService.assignUserToTestModule(request);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error assigning user to test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @DeleteMapping("/testmodules/assign")
    public ResponseEntity<?> removeUserFromTestModule(@Valid @RequestBody ModuleAssignmentRequest request) {
        try {
            User updatedUser = tcmService.removeUserFromTestModule(request);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error removing user from test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA') or hasRole('TESTER')")
    @GetMapping("/testmodules/assigned-to-me")
    public ResponseEntity<?> getTestModulesAssignedToCurrentUser() {
        try {
            List<TestModule> testModules = tcmService.getTestModulesAssignedToCurrentUser();
            List<TestModuleDTO> testModuleDTOs = testModules.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return new ResponseEntity<>(testModuleDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving assigned test modules: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @GetMapping("/testmodules/{moduleId}/assigned-users")
    public ResponseEntity<?> getUsersAssignedToTestModule(@PathVariable Long moduleId) {
        try {
            List<User> users = userRepository.findUsersAssignedToTestModule(moduleId);
            // Convert to DTOs to avoid serialization issues
            List<com.yourproject.tcm.model.dto.UserDTO> userDTOs = users.stream()
                .map(user -> new com.yourproject.tcm.model.dto.UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getOrganizationName(),
                    user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toList())
                ))
                .collect(java.util.stream.Collectors.toList());
            return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving users assigned to test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @GetMapping("/users/by-role/{roleName}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String roleName) {
        try {
            Organization organization = tcmService.getCurrentUserOrganizationObject();
            if (organization == null) {
                return new ResponseEntity<>("Error: Current user has no organization", HttpStatus.BAD_REQUEST);
            }
            List<User> users = userRepository.findByRoleNameAndOrganization(roleName, organization);
            // Convert to DTOs to avoid serialization issues
            List<com.yourproject.tcm.model.dto.UserDTO> userDTOs = users.stream()
                .map(user -> new com.yourproject.tcm.model.dto.UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getOrganizationName(),
                    user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toList())
                ))
                .collect(java.util.stream.Collectors.toList());
            return new ResponseEntity<>(userDTOs, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace(); // Log full stack trace for debugging
            return new ResponseEntity<>("Error retrieving users by role: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    @PostMapping("/testmodules/{moduleId}/regenerate-executions")
    public ResponseEntity<?> regenerateExecutionsForModule(@PathVariable Long moduleId) {
        try {
            tcmService.regenerateExecutionsForModule(moduleId);
            return new ResponseEntity<>("Test executions regenerated successfully for module " + moduleId, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error regenerating test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
package com.yourproject.tcm.controller;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.model.dto.StepResultResponse;
import com.yourproject.tcm.service.TcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final TcmService tcmService;

    @Autowired
    public ApiController(TcmService tcmService) {
        this.tcmService = tcmService;
    }

    // GET /api/projects -> Get all projects.
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = tcmService.getAllProjects();
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

    // POST /api/projects -> Create a new project.
    @PostMapping("/projects")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project savedProject = tcmService.createProject(project);
        return new ResponseEntity<>(savedProject, HttpStatus.CREATED);
    }

    // GET /api/projects/{projectId} -> Get details for one project (with its test suites).
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

    // POST /api/projects/{projectId}/testmodules -> Create a new test module for a project.
    @PostMapping("/projects/{projectId}/testmodules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> createTestModuleForProject(@PathVariable Long projectId, @RequestBody TestModule testModule) {
        try {
            TestModule savedTestModule = tcmService.createTestModuleForProject(projectId, testModule);
            return new ResponseEntity<>(savedTestModule, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/testmodules/{testModuleId} -> Get details for one test module (with its test suites).
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

    // PUT /api/testmodules/{testModuleId} -> Update a test module (name only).
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

    // DELETE /api/testmodules/{testModuleId} -> Delete a test module.
    @DeleteMapping("/testmodules/{testModuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTestModule(@PathVariable Long testModuleId) {
        try {
            tcmService.deleteTestModule(testModuleId);
            return new ResponseEntity<>("Test module deleted successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting test module: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/testmodules/{testModuleId}/testsuites -> Create a new test suite for a test module.
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

    // GET /api/testsuites/{suiteId} -> Get details for one suite (with its test cases).
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

    // PUT /api/testsuites/{suiteId} -> Update a test suite (name only).
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

    // POST /api/testsuites/{suiteId}/testcases -> Create a new test case. This request body should include the list of TestSteps.
    @PostMapping("/testsuites/{suiteId}/testcases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('QA') or hasRole('BA')")
    public ResponseEntity<?> createTestCaseForTestSuite(@PathVariable Long suiteId, @RequestBody TestCase testCase) {
        try {
            TestCase savedTestCase = tcmService.createTestCaseForTestSuite(suiteId, testCase);
            return new ResponseEntity<>(savedTestCase, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating test case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/testcases/{testCaseId} -> Get full details for one test case (with its steps).
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

    // PUT /api/testcases/{testCaseId} -> Update a test case (and its steps).
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

    // DELETE /api/testcases/{testCaseId} -> Delete a test case.
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

    // POST /api/testcases/{testCaseId}/executions -> Start a new test execution for a test case.
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

    // GET /api/executions/{executionId} -> Get execution details.
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

    // GET /api/testcases/{testCaseId}/executions -> Get all executions for a test case.
    @GetMapping("/testcases/{testCaseId}/executions")
    public ResponseEntity<?> getTestExecutionsByTestCaseId(@PathVariable Long testCaseId) {
        try {
            List<TestExecution> executions = tcmService.getTestExecutionsByTestCaseId(testCaseId);
            return new ResponseEntity<>(executions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving test executions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/executions/{executionId}/steps/{stepId} -> Update step result.
    @PutMapping("/executions/{executionId}/steps/{stepId}")
    public ResponseEntity<?> updateStepResult(
            @PathVariable Long executionId,
            @PathVariable Long stepId,
            @RequestBody StepResultRequest stepData) {
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

    // PUT /api/executions/{executionId}/complete -> Complete a test execution.
    @PutMapping("/executions/{executionId}/complete")
    public ResponseEntity<?> completeTestExecution(
            @PathVariable Long executionId,
            @RequestBody ExecutionCompleteRequest completeData) {
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
}

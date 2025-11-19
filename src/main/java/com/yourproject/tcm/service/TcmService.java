package com.yourproject.tcm.service;

import com.yourproject.tcm.model.*;
import com.yourproject.tcm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional
public class TcmService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestModuleRepository testModuleRepository;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private TestStepResultRepository testStepResultRepository;

    // Project methods
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    // TestModule methods
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

    @Transactional
    public TestModule createTestModuleForProject(Long projectId, TestModule testModule) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            testModule.setProject(project);
            TestModule savedTestModule = testModuleRepository.save(testModule);
            entityManager.flush(); // Ensure data is written to DB
            return savedTestModule;
        } else {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
    }

    // Test Suite methods
    @Transactional(readOnly = true)
    public Optional<TestSuite> getTestSuiteById(Long suiteId) {
        return testSuiteRepository.findByIdWithModule(suiteId);
    }

    @Transactional
    public TestSuite createTestSuiteForTestModule(Long testModuleId, TestSuite testSuite) {
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(testModuleId);
        if (testModuleOpt.isPresent()) {
            TestModule testModule = testModuleOpt.get();
            testSuite.setTestModule(testModule);
            TestSuite savedTestSuite = testSuiteRepository.save(testSuite);
            entityManager.flush(); // Ensure data is written to DB
            return savedTestSuite;
        } else {
            throw new RuntimeException("Test Module not found with id: " + testModuleId);
        }
    }

    // Test Case methods
    @Transactional(readOnly = true)
    public Optional<TestCase> getTestCaseById(Long testCaseId) {
        return testCaseRepository.findById(testCaseId);
    }

    @Transactional
    public TestCase createTestCaseForTestSuite(Long suiteId, TestCase testCase) {
        Optional<TestSuite> suiteOpt = testSuiteRepository.findById(suiteId);
        if (suiteOpt.isPresent()) {
            TestSuite testSuite = suiteOpt.get();
            testCase.setTestSuite(testSuite);
            if (testCase.getTestSteps() != null) {
                for (TestStep step : testCase.getTestSteps()) {
                    step.setTestCase(testCase);
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

    @Transactional
    public TestCase updateTestCase(Long testCaseId, TestCase testCaseDetails) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            testCase.setTitle(testCaseDetails.getTitle());
            testCase.setTestCaseId(testCaseDetails.getTestCaseId());
            testCase.setPriority(testCaseDetails.getPriority());
            testCase.setTestSteps(testCaseDetails.getTestSteps());
            TestCase updatedTestCase = testCaseRepository.save(testCase);
            entityManager.flush(); // Ensure data is written to DB
            entityManager.clear(); // Clear the persistence context to avoid stale data
            return updatedTestCase;
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    @Transactional
    public void deleteTestCase(Long testCaseId) {
        if (testCaseRepository.existsById(testCaseId)) {
            testCaseRepository.deleteById(testCaseId);
            entityManager.flush(); // Ensure data is written to DB
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    // Test Execution methods
    @Transactional(readOnly = true)
    public Optional<TestExecution> getTestExecutionById(Long executionId) {
        return testExecutionRepository.findByIdWithStepResults(executionId);
    }

    @Transactional(readOnly = true)
    public List<TestExecution> getTestExecutionsByTestCaseId(Long testCaseId) {
        return testExecutionRepository.findByTestCaseId(testCaseId);
    }

    @Transactional
    public TestExecution createTestExecutionForTestCase(Long testCaseId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);
            execution.setExecutionDate(LocalDateTime.now());
            execution.setOverallResult("Incomplete");

            // Create step results for each step
            if (testCase.getTestSteps() != null) {
                List<TestStepResult> stepResults = testCase.getTestSteps().stream()
                    .map(step -> {
                        TestStepResult result = new TestStepResult();
                        result.setTestExecution(execution);
                        result.setTestStep(step);
                        result.setStepNumber(step.getStepNumber());
                        result.setStatus("Skipped");
                        return result;
                    })
                    .collect(Collectors.toList());
                execution.setStepResults(stepResults);
            }

            TestExecution savedExecution = testExecutionRepository.save(execution);
            entityManager.flush();
            return savedExecution;
        } else {
            throw new RuntimeException("Test Case not found with id: " + testCaseId);
        }
    }

    @Transactional
    public TestStepResult updateStepResult(Long executionId, Long stepId, String status, String actualResult) {
        TestStepResult existingResult = testStepResultRepository.findByTestExecutionIdAndTestStepId(executionId, stepId);
        if (existingResult != null) {
            existingResult.setStatus(status);
            existingResult.setActualResult(actualResult);
            TestStepResult savedResult = testStepResultRepository.save(existingResult);
            entityManager.flush();
            return savedResult;
        } else {
            throw new RuntimeException("Step result not found for execution " + executionId + " and step " + stepId);
        }
    }

    @Transactional
    public TestExecution completeTestExecution(Long executionId, String overallResult, String notes) {
        Optional<TestExecution> executionOpt = testExecutionRepository.findById(executionId);
        if (executionOpt.isPresent()) {
            TestExecution execution = executionOpt.get();
            execution.setOverallResult(overallResult);
            execution.setNotes(notes);
            TestExecution savedExecution = testExecutionRepository.save(execution);
            entityManager.flush();
            return savedExecution;
        } else {
            throw new RuntimeException("Test Execution not found with id: " + executionId);
        }
    }
}

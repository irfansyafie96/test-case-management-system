package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.TestSubmodule;
import com.yourproject.tcm.model.dto.TestExecutionDTO;
import com.yourproject.tcm.repository.TestCaseRepository;
import com.yourproject.tcm.repository.TestSubmoduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TestSubmoduleRepository testSubmoduleRepository;

    @Autowired
    public TestCaseService(TestCaseRepository testCaseRepository, TestSubmoduleRepository testSubmoduleRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testSubmoduleRepository = testSubmoduleRepository;
    }

    /**
     * Get all test cases.
     */
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    /**
     * Get a test case by ID.
     */
    public Optional<TestCase> getTestCaseById(Long testCaseId) {
        return testCaseRepository.findById(testCaseId);
    }

    /**
     * Create a test case for a test submodule.
     */
    @Transactional
    public TestCase createTestCaseForTestSubmodule(Long submoduleId, TestCase testCase) {
        return testSubmoduleRepository.findById(submoduleId).map(submodule -> {
            testCase.setTestSubmodule(submodule);
            return testCaseRepository.save(testCase);
        }).orElse(null);
    }

    /**
     * Update a test case.
     */
    @Transactional
    public TestCase updateTestCase(Long testCaseId, TestCase testCaseDetails) {
        return testCaseRepository.findById(testCaseId).map(testCase -> {
            testCase.setTestCaseId(testCaseDetails.getTestCaseId());
            testCase.setTitle(testCaseDetails.getTitle());
            testCase.setDescription(testCaseDetails.getDescription());
            testCase.setPrerequisites(testCaseDetails.getPrerequisites());
            testCase.setExpectedResult(testCaseDetails.getExpectedResult());
            testCase.setTags(testCaseDetails.getTags());
            
            // Update test steps
            if (testCaseDetails.getTestSteps() != null) {
                testCase.getTestSteps().clear();
                testCase.getTestSteps().addAll(testCaseDetails.getTestSteps());
                testCaseDetails.getTestSteps().forEach(step -> step.setTestCase(testCase));
            }
            
            return testCaseRepository.save(testCase);
        }).orElse(null);
    }

    /**
     * Delete a test case.
     */
    @Transactional
    public void deleteTestCase(Long testCaseId) {
        testCaseRepository.deleteById(testCaseId);
    }

    /**
     * Get test executions for a test case.
     */
    public List<TestExecutionDTO> getTestExecutionsByTestCaseId(Long testCaseId) {
        return testCaseRepository.findById(testCaseId)
            .map(TestCase::getTestExecutions)
            .map(executions -> executions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    /**
     * Create a test execution for a test case.
     */
    @Transactional
    public TestExecution createTestExecutionForTestCase(Long testCaseId) {
        return testCaseRepository.findById(testCaseId).map(testCase -> {
            TestExecution execution = new TestExecution();
            execution.setTestCase(testCase);
            execution.setStatus("PENDING");
            execution.setOverallResult("PENDING");
            
            // Initialize step results from test steps
            if (testCase.getTestSteps() != null) {
                testCase.getTestSteps().forEach(step -> {
                    // Create step results if needed
                });
            }
            
            return testCaseRepository.save(testCase).getTestExecutions().stream()
                .reduce((first, second) -> second)
                .orElse(null);
        }).orElse(null);
    }

    /**
     * Create a test execution for a test case and assign to a specific user.
     */
    @Transactional
    public TestExecution createTestExecutionForTestCaseAndUser(Long testCaseId, Long userId) {
        // Implementation would need UserRepository to fetch the user
        return createTestExecutionForTestCase(testCaseId);
    }

    /**
     * Convert TestExecution to DTO.
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

        return new TestExecutionDTO(
            execution.getId(),
            execution.getTestCaseId(),
            execution.getTestCase() != null ? execution.getTestCase().getTitle() : "",
            execution.getExecutionDate(),
            execution.getOverallResult(),
            execution.getNotes(),
            execution.getDuration(),
            execution.getEnvironment(),
            execution.getExecutedBy(),
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getId() : null,
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getUsername() : "",
            testCase != null ? testCase.getTestSubmoduleId() : null,
            testCase != null ? testCase.getTestSubmoduleName() : null,
            testCase != null ? testCase.getTestModule().getId() : null,
            testCase != null ? testCase.getTestModule().getName() : "",
            testCase != null ? testCase.getTestModule().getProject().getId() : null,
            testCase != null ? testCase.getTestModule().getProject().getName() : "",
            stepResultDTOs
        );
    }
}
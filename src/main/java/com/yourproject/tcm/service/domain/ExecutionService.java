package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.TestStepResult;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.StepResultResponse;
import com.yourproject.tcm.model.dto.TestExecutionDTO;
import com.yourproject.tcm.repository.TestExecutionRepository;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Domain service for TestExecution-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ExecutionService {

    private final TestExecutionRepository testExecutionRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExecutionService(TestExecutionRepository testExecutionRepository, UserRepository userRepository) {
        this.testExecutionRepository = testExecutionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all executions in the organization.
     */
    public List<TestExecutionDTO> getAllExecutionsInOrganization(Long userId) {
        return testExecutionRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get a test execution by ID.
     */
    public Optional<TestExecution> getTestExecutionById(Long executionId) {
        return testExecutionRepository.findById(executionId);
    }

    /**
     * Get test executions assigned to a user.
     */
    public List<TestExecutionDTO> getTestExecutionsAssignedToUser(Long userId) {
        return userRepository.findById(userId)
            .map(User::getTestExecutions)
            .map(executions -> executions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    /**
     * Get test executions for the current user.
     */
    public List<TestExecutionDTO> getTestExecutionsForCurrentUser() {
        // This will need to be implemented with current user context
        return testExecutionRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Complete a test execution.
     */
    @Transactional
    public TestExecution completeTestExecution(Long executionId, String overallResult, String notes) {
        return testExecutionRepository.findById(executionId).map(execution -> {
            execution.setOverallResult(overallResult);
            execution.setNotes(notes);
            execution.setCompletionDate(LocalDateTime.now());
            execution.setStatus("COMPLETED");
            return testExecutionRepository.save(execution);
        }).orElse(null);
    }

    /**
     * Save execution work (notes).
     */
    @Transactional
    public TestExecution saveExecutionWork(Long executionId, String notes) {
        return testExecutionRepository.findById(executionId).map(execution -> {
            execution.setNotes(notes);
            return testExecutionRepository.save(execution);
        }).orElse(null);
    }

    /**
     * Assign a test execution to a user.
     */
    @Transactional
    public TestExecution assignTestExecutionToUser(Long executionId, Long userId) {
        return testExecutionRepository.findById(executionId).map(execution -> {
            return userRepository.findById(userId).map(user -> {
                execution.setAssignedToUser(user);
                execution.setStatus("IN_PROGRESS");
                if (execution.getStartDate() == null) {
                    execution.setStartDate(LocalDateTime.now());
                }
                return testExecutionRepository.save(execution);
            }).orElse(null);
        }).orElse(null);
    }

    /**
     * Update a step result.
     */
    @Transactional
    public StepResultResponse updateStepResult(Long executionId, Long stepId, String status, String actualResult) {
        return testExecutionRepository.findById(executionId).map(execution -> {
            if (execution.getStepResults() != null) {
                for (TestStepResult stepResult : execution.getStepResults()) {
                    if (stepResult.getTestStepId() != null && stepResult.getTestStepId().equals(stepId)) {
                        stepResult.setStatus(status);
                        stepResult.setActualResult(actualResult);
                        testExecutionRepository.save(execution);
                        return new StepResultResponse(
                            stepResult.getId(),
                            stepResult.getTestStep() != null ? stepResult.getTestStep().getId() : null,
                            stepResult.getStepNumber(),
                            stepResult.getActualResult(),
                            stepResult.getStatus(),
                            stepResult.getTestStep().getAction(),
                            stepResult.getTestStep().getExpectedResult()
                        );
                    }
                }
            }
            return null;
        }).orElse(null);
    }

    /**
     * Convert TestExecution to DTO.
     */
    private TestExecutionDTO convertToDTO(TestExecution execution) {
        if (execution.getTestCase() == null) {
            return null;
        }
        
        var testCase = execution.getTestCase();
        var testModule = testCase.getTestModule();
        
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
            testCase.getTitle(),
            execution.getExecutionDate(),
            execution.getOverallResult(),
            execution.getNotes(),
            execution.getDuration(),
            execution.getEnvironment(),
            execution.getExecutedBy(),
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getId() : null,
            execution.getAssignedToUser() != null ? execution.getAssignedToUser().getUsername() : "",
            testCase.getSubmoduleId(),
            testCase.getSubmoduleName(),
            testModule.getId(),
            testModule.getName(),
            testModule.getProject().getId(),
            testModule.getProject().getName(),
            stepResultDTOs
        );
    }
}
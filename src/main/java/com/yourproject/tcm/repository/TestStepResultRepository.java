package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.TestStepResult;

import java.util.List;

public interface TestStepResultRepository extends JpaRepository<TestStepResult, Long> {
    List<TestStepResult> findByTestExecutionId(Long executionId);
    TestStepResult findByTestExecutionIdAndTestStepId(Long executionId, Long stepId);
}

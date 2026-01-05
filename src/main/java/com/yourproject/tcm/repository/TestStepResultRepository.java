package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestStepResult;

import java.util.List;

public interface TestStepResultRepository extends JpaRepository<TestStepResult, Long> {
    List<TestStepResult> findByTestExecution_Id(Long executionId);
    TestStepResult findByTestExecution_IdAndTestStep_Id(Long executionId, Long stepId);

    @Modifying
    @Query("DELETE FROM TestStepResult tsr WHERE tsr.testStep.id = :stepId")
    void deleteByTestStepId(@Param("stepId") Long stepId);
}

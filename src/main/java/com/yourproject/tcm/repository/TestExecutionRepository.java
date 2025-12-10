package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.User;

import java.util.List;
import java.util.Optional;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findByTestCaseId(Long testCaseId);

    @Query("SELECT e FROM TestExecution e LEFT JOIN FETCH e.testCase LEFT JOIN FETCH e.stepResults sr LEFT JOIN FETCH sr.testStep WHERE e.id = :id")
    Optional<TestExecution> findByIdWithStepResults(@Param("id") Long id);

    @Query("SELECT e FROM TestExecution e LEFT JOIN FETCH e.assignedToUser LEFT JOIN FETCH e.testCase LEFT JOIN FETCH e.stepResults sr LEFT JOIN FETCH sr.testStep WHERE e.assignedToUser = :user")
    List<TestExecution> findByAssignedToUserWithDetails(@Param("user") User user);

    List<TestExecution> findByAssignedToUser(User user);
}

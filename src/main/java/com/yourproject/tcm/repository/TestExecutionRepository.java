package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.User;

import java.util.List;
import java.util.Optional;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findByTestCase_Id(Long testCaseId);

    @Query("SELECT DISTINCT e FROM TestExecution e " +
           "LEFT JOIN FETCH e.testCase tc " +
           "LEFT JOIN FETCH e.assignedToUser " +
           "LEFT JOIN FETCH e.stepResults sr " +
           "LEFT JOIN FETCH sr.testStep " +
           "LEFT JOIN FETCH tc.submodule ts " +
           "LEFT JOIN FETCH ts.testModule tm " +
           "LEFT JOIN FETCH tm.project p " +
           "LEFT JOIN FETCH p.organization " +
           "WHERE e.id = :id")
    Optional<TestExecution> findByIdWithStepResults(@Param("id") Long id);

    @Query("SELECT e FROM TestExecution e " +
           "LEFT JOIN FETCH e.assignedToUser " +
           "LEFT JOIN FETCH e.testCase tc " +
           "LEFT JOIN FETCH tc.submodule ts " +
           "LEFT JOIN FETCH ts.testModule tm " +
           "LEFT JOIN FETCH tm.project p " +
           "LEFT JOIN FETCH e.stepResults sr " +
           "LEFT JOIN FETCH sr.testStep " +
           "WHERE e.assignedToUser = :user")
    List<TestExecution> findByAssignedToUserWithDetails(@Param("user") User user);

    List<TestExecution> findByAssignedToUser(User user);
    @Query("SELECT e FROM TestExecution e LEFT JOIN FETCH e.assignedToUser LEFT JOIN FETCH e.testCase tc LEFT JOIN FETCH tc.submodule ts LEFT JOIN FETCH ts.testModule tm LEFT JOIN FETCH tm.project p LEFT JOIN FETCH e.stepResults sr LEFT JOIN FETCH sr.testStep")
    List<TestExecution> findAllWithDetails();

    @Query("SELECT e FROM TestExecution e " +
           "LEFT JOIN FETCH e.assignedToUser " +
           "LEFT JOIN FETCH e.testCase tc " +
           "LEFT JOIN FETCH tc.submodule ts " +
           "LEFT JOIN FETCH ts.testModule tm " +
           "LEFT JOIN FETCH tm.project p " +
           "LEFT JOIN FETCH e.stepResults sr " +
           "LEFT JOIN FETCH sr.testStep " +
           "WHERE p.organization.id = :organizationId")
    List<TestExecution> findAllWithDetailsByOrganizationId(@Param("organizationId") Long organizationId);
}

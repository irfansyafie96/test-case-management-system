package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.TestModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface TestModuleRepository extends JpaRepository<TestModule, Long> {
    @Query("SELECT tm FROM TestModule tm LEFT JOIN FETCH tm.submodules WHERE tm.id = :id")
    Optional<TestModule> findByIdWithSubmodules(@Param("id") Long id);

    @Query("SELECT tm FROM TestModule tm LEFT JOIN FETCH tm.submodules WHERE tm.id = :id")
    Optional<TestModule> findByIdWithSuitesAndCasesAndSteps(@Param("id") Long id);

    @Query("SELECT tm FROM TestModule tm LEFT JOIN FETCH tm.assignedUsers WHERE tm.id = :id")
    Optional<TestModule> findByIdWithAssignedUsers(@Param("id") Long id);

    // Find test modules assigned to a specific user (Directly OR via Project assignment)
    @Query("SELECT DISTINCT tm FROM TestModule tm " +
           "JOIN FETCH tm.project p " +
           "LEFT JOIN tm.assignedUsers u " +
           "LEFT JOIN p.assignedUsers pu " +
           "WHERE u.id = :userId OR pu.id = :userId")
    List<TestModule> findTestModulesAssignedToUser(@Param("userId") Long userId);

    // Find test modules NOT assigned to a specific user (for assignment purposes)
    @Query("SELECT tm FROM TestModule tm JOIN FETCH tm.project WHERE tm.id NOT IN (SELECT tm2.id FROM TestModule tm2 JOIN tm2.assignedUsers u WHERE u.id = :userId)")
    List<TestModule> findTestModulesNotAssignedToUser(@Param("userId") Long userId);

    // Find test modules in a specific project assigned to a user
    @Query("SELECT DISTINCT tm FROM TestModule tm JOIN FETCH tm.project JOIN tm.assignedUsers u WHERE u.id = :userId AND tm.project.id = :projectId")
    List<TestModule> findTestModulesAssignedToUserInProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
    // Find test modules in projects assigned to a user (for QA/BA users)
    @Query("SELECT DISTINCT tm FROM TestModule tm JOIN FETCH tm.project p JOIN p.assignedUsers u WHERE u.id = :userId")
    List<TestModule> findTestModulesInProjectsAssignedToUser(@Param("userId") Long userId);
    @Query("SELECT DISTINCT tm FROM TestModule tm " +
           "JOIN FETCH tm.project")
    List<TestModule> findAll();
    
    // Check if a user is assigned to a test module (direct assignment)
    @Query("SELECT CASE WHEN COUNT(u.id) > 0 THEN true ELSE false END FROM TestModule tm " +
           "JOIN tm.assignedUsers u WHERE tm.id = :moduleId AND u.id = :userId")
    boolean isUserAssignedToModule(@Param("moduleId") Long moduleId, @Param("userId") Long userId);
}
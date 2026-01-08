package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.modules WHERE p.id = :id")
    Optional<Project> findProjectWithModulesById(@Param("id") Long id);

    // Find projects assigned to a specific user (Directly OR via Module assignment)
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN p.modules m " +
           "LEFT JOIN p.assignedUsers pa " +
           "LEFT JOIN m.assignedUsers ma " +
           "WHERE pa.id = :userId OR ma.id = :userId")
    List<Project> findProjectsAssignedToUser(@Param("userId") Long userId);

    // Find projects NOT assigned to a specific user (for assignment purposes)
    @Query("SELECT p FROM Project p WHERE p.id NOT IN (SELECT p2.id FROM Project p2 JOIN p2.assignedUsers u WHERE u.id = :userId)")
    List<Project> findProjectsNotAssignedToUser(@Param("userId") Long userId);
}
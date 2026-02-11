package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.ExecutionAssignee;
import com.yourproject.tcm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ExecutionAssignee entity
 * 
 * Manages data access for execution assignee assignments (QA/BA/TESTER).
 */
@Repository
public interface ExecutionAssigneeRepository extends JpaRepository<ExecutionAssignee, Long> {
    
    /**
     * Find all execution assignee assignments for a specific test module
     */
    List<ExecutionAssignee> findByTestModuleId(Long testModuleId);
    
    /**
     * Find all execution assignee assignments for a specific user
     */
    List<ExecutionAssignee> findByUserId(Long userId);
    
    /**
     * Find a specific execution assignee assignment
     */
    Optional<ExecutionAssignee> findByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Delete a specific execution assignee assignment
     */
    void deleteByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Check if a user is assigned as an execution assignee for a specific module
     */
    boolean existsByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Find all execution assignees for a specific test module, with eager loading of roles
     */
    @Query("SELECT ea.user FROM ExecutionAssignee ea " +
           "LEFT JOIN FETCH ea.user.roles " +
           "WHERE ea.testModule.id = :testModuleId " +
           "ORDER BY ea.user.username")
    List<User> findAssigneesByModuleIdWithRoles(@Param("testModuleId") Long testModuleId);
    
    /**
     * Find all modules where a user is assigned as an execution assignee
     */
    @Query("SELECT ea.testModule FROM ExecutionAssignee ea " +
           "WHERE ea.user.id = :userId")
    List<com.yourproject.tcm.model.TestModule> findModulesByUserId(@Param("userId") Long userId);
}
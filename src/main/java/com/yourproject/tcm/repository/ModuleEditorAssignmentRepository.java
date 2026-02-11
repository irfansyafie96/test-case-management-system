package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.ModuleEditorAssignment;
import com.yourproject.tcm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ModuleEditorAssignment entity
 * 
 * Manages data access for module editor assignments (QA/BA only).
 */
@Repository
public interface ModuleEditorAssignmentRepository extends JpaRepository<ModuleEditorAssignment, Long> {
    
    /**
     * Find all module editor assignments for a specific test module
     */
    List<ModuleEditorAssignment> findByTestModuleId(Long testModuleId);
    
    /**
     * Find all module editor assignments for a specific user
     */
    List<ModuleEditorAssignment> findByUserId(Long userId);
    
    /**
     * Find a specific module editor assignment
     */
    Optional<ModuleEditorAssignment> findByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Delete a specific module editor assignment
     */
    void deleteByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Check if a user is assigned as a module editor for a specific module
     */
    boolean existsByUserIdAndTestModuleId(Long userId, Long testModuleId);
    
    /**
     * Find all module editors for a specific test module, with eager loading of roles
     */
    @Query("SELECT mea.user FROM ModuleEditorAssignment mea " +
           "LEFT JOIN FETCH mea.user.roles " +
           "WHERE mea.testModule.id = :testModuleId " +
           "ORDER BY mea.user.username")
    List<User> findEditorsByModuleIdWithRoles(@Param("testModuleId") Long testModuleId);
    
    /**
     * Find all modules where a user is assigned as a module editor
     */
    @Query("SELECT mea.testModule FROM ModuleEditorAssignment mea " +
           "WHERE mea.user.id = :userId")
    List<com.yourproject.tcm.model.TestModule> findModulesByUserId(@Param("userId") Long userId);
}
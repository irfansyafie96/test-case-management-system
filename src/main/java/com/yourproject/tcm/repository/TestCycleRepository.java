package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.TestCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCycleRepository extends JpaRepository<TestCycle, Long> {
    
    List<TestCycle> findByProjectIdOrderBySortOrderAsc(Long projectId);
    
    List<TestCycle> findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(Long projectId);
    
    Optional<TestCycle> findByProjectIdAndIsActiveTrueAndId(Long projectId, Long cycleId);
    
    @Query("SELECT COALESCE(MAX(tc.sortOrder), 0) FROM TestCycle tc WHERE tc.project.id = :projectId")
    int findMaxSortOrderByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT tc FROM TestCycle tc WHERE tc.project.id = :projectId AND tc.isActive = true ORDER BY tc.sortOrder ASC")
    List<TestCycle> findActiveCyclesByProjectId(@Param("projectId") Long projectId);
    
    @Modifying
    @Query("UPDATE TestCycle tc SET tc.isActive = false WHERE tc.project.id = :projectId AND tc.id != :excludeCycleId")
    void deactivateOtherCycles(@Param("projectId") Long projectId, @Param("excludeCycleId") Long excludeCycleId);
}

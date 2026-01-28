package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.Submodule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmoduleRepository extends JpaRepository<Submodule, Long> {
    @Query("SELECT s FROM Submodule s LEFT JOIN FETCH s.testModule WHERE s.id = :id")
    Optional<Submodule> findByIdWithModule(@Param("id") Long id);

    @Query("SELECT s FROM Submodule s LEFT JOIN FETCH s.testCases WHERE s.testModule.id = :moduleId ORDER BY s.id ASC")
    List<Submodule> findByTestModuleIdWithTestCases(@Param("moduleId") Long moduleId);

    List<Submodule> findByTestModule_Id(Long moduleId);

    @Modifying
    @Query("DELETE FROM Submodule s WHERE s.testModule.id = :moduleId")
    void deleteByTestModuleId(@Param("moduleId") Long moduleId);
}
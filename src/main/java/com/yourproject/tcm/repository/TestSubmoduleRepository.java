package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.TestSubmodule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestSubmoduleRepository extends JpaRepository<TestSubmodule, Long> {
    @Query("SELECT ts FROM TestSubmodule ts LEFT JOIN FETCH ts.testModule WHERE ts.id = :id")
    Optional<TestSubmodule> findByIdWithModule(@Param("id") Long id);

    @Query("SELECT ts FROM TestSubmodule ts LEFT JOIN FETCH ts.testCases WHERE ts.testModule.id = :moduleId ORDER BY ts.id ASC")
    List<TestSubmodule> findByTestModuleIdWithTestCases(@Param("moduleId") Long moduleId);

    List<TestSubmodule> findByTestModule_Id(Long moduleId);

    @Modifying
    @Query("DELETE FROM TestSubmodule ts WHERE ts.testModule.id = :moduleId")
    void deleteByTestModuleId(@Param("moduleId") Long moduleId);
}
package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestModule;

import java.util.Optional;

public interface TestModuleRepository extends JpaRepository<TestModule, Long> {
    @Query("SELECT tm FROM TestModule tm LEFT JOIN FETCH tm.testSuites WHERE tm.id = :id")
    Optional<TestModule> findByIdWithTestSuites(@Param("id") Long id);

    @Query("SELECT tm FROM TestModule tm LEFT JOIN FETCH tm.testSuites WHERE tm.id = :id")
    Optional<TestModule> findByIdWithSuitesAndCasesAndSteps(@Param("id") Long id);
}
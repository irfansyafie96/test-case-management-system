package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    @Query("SELECT ts FROM TestSuite ts LEFT JOIN FETCH ts.testModule WHERE ts.id = :id")
    Optional<TestSuite> findByIdWithModule(@Param("id") Long id);

    @Query("SELECT ts FROM TestSuite ts LEFT JOIN FETCH ts.testCases WHERE ts.testModule.id = :moduleId")
    List<TestSuite> findByTestModuleIdWithTestCases(@Param("moduleId") Long moduleId);
}
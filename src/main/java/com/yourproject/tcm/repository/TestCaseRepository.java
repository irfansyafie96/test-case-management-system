package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    @Query("SELECT DISTINCT tc FROM TestCase tc LEFT JOIN FETCH tc.testSteps WHERE tc.id = :id")
    TestCase findByIdWithSteps(@Param("id") Long id);
    
    @Query("SELECT DISTINCT tc FROM TestCase tc LEFT JOIN FETCH tc.testSteps LEFT JOIN FETCH tc.testSuite")
    java.util.List<TestCase> findAll();
    
    @Query("SELECT tc FROM TestCase tc LEFT JOIN FETCH tc.testSteps WHERE tc.testSuite.testModule.id = :moduleId")
    java.util.List<TestCase> findByModuleIdWithSteps(@Param("moduleId") Long moduleId);
}
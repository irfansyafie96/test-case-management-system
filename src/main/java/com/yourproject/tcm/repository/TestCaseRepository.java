package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yourproject.tcm.model.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    @Query("SELECT DISTINCT tc FROM TestCase tc LEFT JOIN FETCH tc.testSteps WHERE tc.id = :id")
    TestCase findByIdWithSteps(@Param("id") Long id);
    
    @Query("SELECT DISTINCT tc FROM TestCase tc " +
           "LEFT JOIN FETCH tc.testSteps " +
           "LEFT JOIN FETCH tc.testSubmodule ts " +
           "LEFT JOIN FETCH ts.testModule tm " +
           "LEFT JOIN FETCH tm.project p")
    java.util.List<TestCase> findAllWithDetails();

    @Query("SELECT DISTINCT tc FROM TestCase tc " +
           "LEFT JOIN FETCH tc.testSteps " +
           "LEFT JOIN FETCH tc.testSubmodule ts " +
           "LEFT JOIN FETCH ts.testModule tm " +
           "LEFT JOIN FETCH tm.project p " +
           "WHERE p.organization.id = :organizationId")
    java.util.List<TestCase> findAllWithDetailsByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT tc FROM TestCase tc LEFT JOIN FETCH tc.testSteps WHERE tc.testSubmodule.testModule.id = :moduleId")
    java.util.List<TestCase> findByModuleIdWithSteps(@Param("moduleId") Long moduleId);
}
package com.yourproject.tcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> { }
package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.TestSubmodule;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.repository.TestSubmoduleRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for TestSubmodule-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class SubmoduleService {

    private final TestSubmoduleRepository testSubmoduleRepository;
    private final TestModuleRepository testModuleRepository;

    @Autowired
    public SubmoduleService(TestSubmoduleRepository testSubmoduleRepository, TestModuleRepository testModuleRepository) {
        this.testSubmoduleRepository = testSubmoduleRepository;
        this.testModuleRepository = testModuleRepository;
    }

    /**
     * Get a test submodule by ID.
     */
    public Optional<TestSubmodule> getTestSubmoduleById(Long submoduleId) {
        return testSubmoduleRepository.findByIdWithModule(submoduleId);
    }

    /**
     * Get all test submodules for a test module.
     */
    public List<TestSubmodule> getTestSubmodulesByModuleId(Long moduleId) {
        return testSubmoduleRepository.findByTestModuleIdWithTestCases(moduleId);
    }

    /**
     * Create a test submodule for a test module.
     */
    @Transactional
    public TestSubmodule createTestSubmoduleForTestModule(Long testModuleId, TestSubmodule testSubmodule) {
        return testModuleRepository.findById(testModuleId).map(testModule -> {
            testSubmodule.setTestModule(testModule);
            return testSubmoduleRepository.save(testSubmodule);
        }).orElse(null);
    }

    /**
     * Update a test submodule.
     */
    @Transactional
    public TestSubmodule updateTestSubmodule(Long submoduleId, TestSubmodule submoduleDetails) {
        return testSubmoduleRepository.findById(submoduleId).map(submodule -> {
            submodule.setName(submoduleDetails.getName());
            return testSubmoduleRepository.save(submodule);
        }).orElse(null);
    }

    /**
     * Delete a test submodule.
     */
    @Transactional
    public void deleteTestSubmodule(Long submoduleId) {
        testSubmoduleRepository.deleteById(submoduleId);
    }
}
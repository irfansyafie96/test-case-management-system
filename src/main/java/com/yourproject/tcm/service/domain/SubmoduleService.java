package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.repository.SubmoduleRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for Submodule-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class SubmoduleService {

    private final SubmoduleRepository submoduleRepository;
    private final TestModuleRepository testModuleRepository;

    @Autowired
    public SubmoduleService(SubmoduleRepository submoduleRepository, TestModuleRepository testModuleRepository) {
        this.submoduleRepository = submoduleRepository;
        this.testModuleRepository = testModuleRepository;
    }

    /**
     * Get a submodule by ID.
     */
    public Optional<Submodule> getSubmoduleById(Long submoduleId) {
        return submoduleRepository.findByIdWithModule(submoduleId);
    }

    /**
     * Get all submodules for a test module.
     */
    public List<Submodule> getSubmodulesByModuleId(Long moduleId) {
        return submoduleRepository.findByTestModuleIdWithTestCases(moduleId);
    }

    /**
     * Create a submodule for a test module.
     */
    @Transactional
    public Submodule createSubmoduleForTestModule(Long testModuleId, Submodule submodule) {
        return testModuleRepository.findById(testModuleId).map(testModule -> {
            submodule.setTestModule(testModule);
            return submoduleRepository.save(submodule);
        }).orElse(null);
    }

    /**
     * Update a submodule.
     */
    @Transactional
    public Submodule updateSubmodule(Long submoduleId, Submodule submoduleDetails) {
        return submoduleRepository.findById(submoduleId).map(submodule -> {
            submodule.setName(submoduleDetails.getName());
            return submoduleRepository.save(submodule);
        }).orElse(null);
    }

    /**
     * Delete a submodule.
     */
    @Transactional
    public void deleteSubmodule(Long submoduleId) {
        submoduleRepository.deleteById(submoduleId);
    }
}
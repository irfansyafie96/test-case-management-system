package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.ModuleAssignmentRequest;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for TestModule-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ModuleService {

    private final TestModuleRepository testModuleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ModuleService(TestModuleRepository testModuleRepository, UserRepository userRepository) {
        this.testModuleRepository = testModuleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all test modules in the organization.
     */
    public List<TestModule> getAllModulesInOrganization() {
        return testModuleRepository.findAll();
    }

    /**
     * Get test modules assigned to the current user.
     */
    public List<TestModule> getTestModulesAssignedToCurrentUser() {
        // This will need to be implemented with current user context
        return testModuleRepository.findAll();
    }

    /**
     * Get a test module by ID.
     */
    public Optional<TestModule> getTestModuleById(Long testModuleId) {
        return testModuleRepository.findById(testModuleId);
    }

    /**
     * Create a test module for a project.
     */
    @Transactional
    public TestModule createTestModuleForProject(Long projectId, TestModule testModule) {
        return testModuleRepository.save(testModule);
    }

    /**
     * Update a test module.
     */
    @Transactional
    public TestModule updateTestModule(Long testModuleId, TestModule testModuleDetails) {
        return testModuleRepository.findById(testModuleId).map(testModule -> {
            testModule.setName(testModuleDetails.getName());
            testModule.setDescription(testModuleDetails.getDescription());
            return testModuleRepository.save(testModule);
        }).orElse(null);
    }

    /**
     * Delete a test module.
     */
    @Transactional
    public void deleteTestModule(Long testModuleId) {
        testModuleRepository.deleteById(testModuleId);
    }

    /**
     * Assign a user to a test module.
     */
    @Transactional
    public User assignUserToTestModule(ModuleAssignmentRequest request) {
        return testModuleRepository.findById(request.getTestModuleId()).map(testModule -> {
            return userRepository.findById(request.getUserId()).map(user -> {
                if (!user.getAssignedTestModules().contains(testModule)) {
                    user.getAssignedTestModules().add(testModule);
                    return userRepository.save(user);
                }
                return user;
            }).orElse(null);
        }).orElse(null);
    }

    /**
     * Remove a user from a test module.
     */
    @Transactional
    public User removeUserFromTestModule(ModuleAssignmentRequest request) {
        return testModuleRepository.findById(request.getTestModuleId()).map(testModule -> {
            return userRepository.findById(request.getUserId()).map(user -> {
                user.getAssignedTestModules().remove(testModule);
                return userRepository.save(user);
            }).orElse(null);
        }).orElse(null);
    }
}
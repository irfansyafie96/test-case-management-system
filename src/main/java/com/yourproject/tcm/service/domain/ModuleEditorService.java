package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.ModuleEditorAssignment;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.ModuleEditorRequest;
import com.yourproject.tcm.repository.ModuleEditorAssignmentRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ModuleEditorService
 * 
 * Service for managing module editor assignments.
 * Only QA and BA users can be assigned as module editors for editing test cases.
 * 
 * This service is separate from ExecutionAssignmentService to maintain clear separation
 * between module editing permissions and test execution assignments.
 */
@Service
@Transactional
public class ModuleEditorService {

    private final ModuleEditorAssignmentRepository moduleEditorAssignmentRepository;
    private final UserRepository userRepository;
    private final TestModuleRepository testModuleRepository;
    private final UserContextService userContextService;
    private final EntityManager entityManager;

    @Autowired
    public ModuleEditorService(
            ModuleEditorAssignmentRepository moduleEditorAssignmentRepository,
            UserRepository userRepository,
            TestModuleRepository testModuleRepository,
            UserContextService userContextService,
            EntityManager entityManager) {
        this.moduleEditorAssignmentRepository = moduleEditorAssignmentRepository;
        this.userRepository = userRepository;
        this.testModuleRepository = testModuleRepository;
        this.userContextService = userContextService;
        this.entityManager = entityManager;
    }

    /**
     * Assign a user as a module editor.
     * Only QA and BA users can be assigned as module editors.
     * 
     * @param request The assignment request containing userId and testModuleId
     * @return The updated user with their assigned modules
     * @throws RuntimeException if user not found, module not found, or user is not QA/BA
     */
    public User assignEditor(ModuleEditorRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        // Fetch test module
        Optional<TestModule> testModuleOpt = testModuleRepository.findById(request.getTestModuleId());
        if (testModuleOpt.isEmpty()) {
            throw new RuntimeException("Test module not found with id: " + request.getTestModuleId());
        }
        TestModule testModule = testModuleOpt.get();

        // Fetch user to assign
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + request.getUserId());
        }
        User user = userOpt.get();

        // Validate user is QA or BA
        validateUserIsEditor(user);

        // Check organization boundary
        if (!user.getOrganization().getId().equals(currentUser.getOrganization().getId()) ||
            !testModule.getProject().getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("User and test module must belong to the same organization as the assigner");
        }

        // Check if assignment already exists
        if (moduleEditorAssignmentRepository.existsByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId())) {
            throw new RuntimeException("User is already assigned as a module editor for this module");
        }

        // Create assignment
        ModuleEditorAssignment assignment = new ModuleEditorAssignment(user, testModule, currentUser.getUsername());
        moduleEditorAssignmentRepository.save(assignment);
        entityManager.flush();

        // Refresh user to get updated assignments
        User refreshedUser = userRepository.findById(user.getId()).orElse(user);
        return refreshedUser;
    }

    /**
     * Remove a user as a module editor.
     * 
     * @param request The removal request containing userId and testModuleId
     * @return The updated user
     * @throws RuntimeException if assignment not found
     */
    public User removeEditor(ModuleEditorRequest request) {
        User currentUser = userContextService.getCurrentUser();
        
        // Check if assignment exists
        if (!moduleEditorAssignmentRepository.existsByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId())) {
            throw new RuntimeException("User is not assigned as a module editor for this module");
        }

        // Delete assignment
        moduleEditorAssignmentRepository.deleteByUserIdAndTestModuleId(
                request.getUserId(), request.getTestModuleId());
        entityManager.flush();

        // Refresh user to get updated assignments
        User refreshedUser = userRepository.findById(request.getUserId()).orElse(null);
        return refreshedUser;
    }

    /**
     * Get all module editors for a specific test module.
     * 
     * @param testModuleId The test module ID
     * @return List of users assigned as module editors
     */
    public List<User> getModuleEditors(Long testModuleId) {
        return moduleEditorAssignmentRepository.findEditorsByModuleIdWithRoles(testModuleId);
    }

    /**
     * Validate that a user is QA or BA.
     * 
     * @param user The user to validate
     * @throws RuntimeException if user is not QA or BA
     */
    private void validateUserIsEditor(User user) {
        if (!userContextService.isQaOrBa(user)) {
            throw new RuntimeException("Only QA and BA users can be assigned as module editors. User: " + 
                    user.getUsername() + " has roles: " + 
                    user.getRoles().stream().map(r -> r.getName()).reduce((a, b) -> a + ", " + b).orElse("none"));
        }
    }
}
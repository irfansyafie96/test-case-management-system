package com.yourproject.tcm.service;

import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for accessing current user context and security checks.
 * Extracted from TcmService to be shared across domain services.
 */
@Service
public class UserContextService {

    private final UserRepository userRepository;

    @Autowired
    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user.
     * @return the current User entity
     * @throws RuntimeException if no authenticated user is found
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Get the currently authenticated user with assigned modules loaded.
     * Use this when you need to access the user's assigned test modules (for editing and execution).
     * @return the current User entity with assignedModulesForEditing and assignedModulesForExecution loaded
     * @throws RuntimeException if no authenticated user is found
     */
    public User getCurrentUserWithModules() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            return userRepository.findByUsernameWithModules(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
        }
        throw new RuntimeException("No authenticated user found");
    }

    /**
     * Get the organization of the currently authenticated user.
     * @return the current user's Organization entity
     */
    public Organization getCurrentUserOrganizationObject() {
        return getCurrentUser().getOrganization();
    }

    /**
     * Check if a user has ADMIN role.
     * @param user the user to check
     * @return true if the user has ADMIN role
     */
    public boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
    }

    /**
     * Check if a user has QA or BA role.
     * @param user the user to check
     * @return true if the user has QA or BA role
     */
    public boolean isQaOrBa(User user) {
        return user.getRoles().stream().anyMatch(role ->
            role.getName().equals("QA") || role.getName().equals("BA"));
    }

    /**
     * Check if a user has TESTER role.
     * @param user the user to check
     * @return true if the user has TESTER role
     */
    public boolean isTester(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals("TESTER"));
    }

    /**
     * Check if a user has PROJECT_MANAGER role.
     * @param user the user to check
     * @return true if the user has PROJECT_MANAGER role
     */
    public boolean isProjectManager(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals("PROJECT_MANAGER"));
    }

    /**
     * Check if the current user has ADMIN role.
     * @return true if the current user has ADMIN role
     */
    public boolean currentUserIsAdmin() {
        return isAdmin(getCurrentUser());
    }

    /**
     * Check if the current user has QA or BA role.
     * @return true if the current user has QA or BA role
     */
    public boolean currentUserIsQaOrBa() {
        return isQaOrBa(getCurrentUser());
    }

    /**
     * Check if the current user has TESTER role.
     * @return true if the current user has TESTER role
     */
    public boolean currentUserIsTester() {
        return isTester(getCurrentUser());
    }

    /**
     * Check if the current user has PROJECT_MANAGER role.
     * @return true if the current user has PROJECT_MANAGER role
     */
    public boolean currentUserIsProjectManager() {
        return isProjectManager(getCurrentUser());
    }

    /**
     * Check if user can create or edit modules in assigned projects.
     * Returns true for ADMIN and PROJECT_MANAGER.
     * @param user the user to check
     * @return true if user can create/edit modules
     */
    public boolean canCreateOrEditModule(User user) {
        return isAdmin(user) || isProjectManager(user);
    }
}
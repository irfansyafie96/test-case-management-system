package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.Role;
import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.repository.RoleRepository;
import com.yourproject.tcm.service.UserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service for User-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserContextService userContextService;

    @Autowired
    public UserService(UserRepository userRepository, 
                       RoleRepository roleRepository,
                       UserContextService userContextService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userContextService = userContextService;
    }

    /**
     * Get all users in the current user's organization.
     * Only ADMIN users can access this.
     * @return List of users in the organization (excluding admin users)
     */
    @Transactional(readOnly = true)
    public List<User> getUsersInOrganization() {
        User currentUser = userContextService.getCurrentUser();
        
        // Only admin/PM users can access this
        if (!userContextService.isAdmin(currentUser) && !userContextService.isProjectManager(currentUser)) {
            throw new RuntimeException("Only admin and project manager users can access organization users");
        }

        Organization org = currentUser.getOrganization();
        if (org == null) {
            throw new RuntimeException("User does not belong to any organization");
        }

        // ADMIN sees all users, PM sees only users in their assigned projects
        if (userContextService.isProjectManager(currentUser)) {
            Set<Long> assignedProjectIds = currentUser.getAssignedProjects().stream()
                .map(Project::getId)
                .collect(Collectors.toSet());
            
            List<User> allUsers = userRepository.findAllNonAdminUsers(org);
            return allUsers.stream()
                .filter(user -> {
                    // Include users assigned to PM's projects
                    if (user.getAssignedProjects() != null) {
                        return user.getAssignedProjects().stream()
                            .anyMatch(p -> assignedProjectIds.contains(p.getId()));
                    }
                    return false;
                })
                .collect(Collectors.toList());
        }

        return userRepository.findAllNonAdminUsers(org);
    }

    /**
     * Update a user's role.
     * ADMIN can change to any role.
     * PROJECT_MANAGER can change to QA, BA, TESTER (not ADMIN).
     */
    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        User currentUser = userContextService.getCurrentUser();
        boolean isAdmin = userContextService.isAdmin(currentUser);
        boolean isProjectManager = userContextService.currentUserIsProjectManager();
        
        if (!isAdmin && !isProjectManager) {
            throw new RuntimeException("Only admin and project manager users can update user roles");
        }

        User userToUpdate = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Security: Ensure the user belongs to the same organization
        if (!userToUpdate.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("You cannot update a user from another organization");
        }

        // Prevent self-role change
        if (userToUpdate.getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot change your own role");
        }

        // Prevent PROJECT_MANAGER from changing ADMIN user's role
        if (isProjectManager && userContextService.isAdmin(userToUpdate)) {
            throw new RuntimeException("Project managers cannot change ADMIN user roles");
        }

        // PROJECT_MANAGER can only assign QA, BA, TESTER (not ADMIN)
        if (isProjectManager && roleName.equals("ADMIN")) {
            throw new RuntimeException("Project managers cannot assign ADMIN role");
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        userToUpdate.setRoles(roles);

        return userRepository.save(userToUpdate);
    }

    /**
     * Deactivate a user (soft remove from team).
     * Only ADMIN users can perform this.
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only admin users can remove users");
        }

        User userToDeactivate = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Security: Ensure the user belongs to the same organization
        if (!userToDeactivate.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("You cannot remove a user from another organization");
        }

        // Prevent self-deactivation
        if (userToDeactivate.getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot remove yourself");
        }

        // Soft delete: disable the user and clear assignments
        userToDeactivate.setEnabled(false);
        userToDeactivate.getAssignedProjects().clear();
        userToDeactivate.getAssignedModulesForEditing().clear();
        userToDeactivate.getAssignedModulesForExecution().clear();

        userRepository.save(userToDeactivate);
    }
}
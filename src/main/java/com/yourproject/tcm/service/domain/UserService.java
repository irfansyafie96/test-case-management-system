package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Domain service for User-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserContextService userContextService;

    @Autowired
    public UserService(UserRepository userRepository, UserContextService userContextService) {
        this.userRepository = userRepository;
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
        
        // Only admin users can access this
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only admin users can access organization users");
        }

        Organization org = currentUser.getOrganization();
        if (org == null) {
            throw new RuntimeException("User does not belong to any organization");
        }

        return userRepository.findAllNonAdminUsers(org);
    }
}
package com.yourproject.tcm.security;

import com.yourproject.tcm.model.User;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl - Spring Security User Details Service
 *
 * This service implements Spring Security's UserDetailsService interface
 * and is responsible for loading user-specific data during authentication.
 *
 * It's called by Spring Security when:
 * 1. Validating credentials during login (AuthController calls authentication manager)
 * 2. Loading user details from JWT token (via AuthTokenFilter when validating requests)
 *
 * The service retrieves user information from the database and converts
 * it to Spring Security's UserDetails format via UserPrincipal.create().
 */
@Service  // Spring service component that can be injected
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;  // Repository for user database operations

    /**
     * Load user details by username from database
     * This method is called by Spring Security during authentication
     *
     * @param username Username to look up in database
     * @return UserDetails object containing user information (username, password, authorities)
     * @throws UsernameNotFoundException if user doesn't exist in database
     */
    @Override
    @Transactional  // Ensure database transaction is handled properly
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Query database for user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Convert database User entity to Spring Security's UserPrincipal format
        // UserPrincipal contains the information Spring Security needs (username, roles, enabled status, etc.)
        return UserPrincipal.create(user);
    }
}

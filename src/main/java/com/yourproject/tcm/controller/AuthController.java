package com.yourproject.tcm.controller;

import com.yourproject.tcm.model.Role;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.JwtResponse;
import com.yourproject.tcm.model.dto.LoginRequest;
import com.yourproject.tcm.model.dto.SignupRequest;
import com.yourproject.tcm.repository.RoleRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.security.JwtUtils;
import com.yourproject.tcm.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AuthController - Handles Authentication Requests (Login/Signup)
 *
 * This controller manages user authentication including:
 * 1. Login: Validates credentials and returns JWT token
 * 2. Signup: Creates new users with appropriate roles
 *
 * JWT (JSON Web Token) Flow:
 * - User sends credentials to /api/auth/login
 * - Backend verifies credentials against database
 * - If valid, backend generates JWT token with user info
 * - Frontend stores token and sends with each authenticated request
 * - Backend validates token on each request to verify identity
 */

/**
 * Enables Cross-Origin Resource Sharing (CORS) for this endpoint.
 *
 * origins = "*" (Wildcard): Allows this resource to be accessed by
 * web clients running on ANY domain. (Use with caution in production.)
 *
 * maxAge = 3600 (1 hour): Sets how long the client browser can cache
 * the result of the CORS preflight OPTIONS request, improving performance.
 */

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")  // All endpoints in this controller start with /api/auth
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;  // Spring Security's authentication manager

    @Autowired
    UserRepository userRepository;  // Repository for user database operations

    @Autowired
    RoleRepository roleRepository;  // Repository for role database operations

    @Autowired
    PasswordEncoder encoder;  // For encoding/hashing passwords

    @Autowired
    JwtUtils jwtUtils;  // Utility class for JWT token operations

    /**
     * POST /api/auth/login - Authenticate user and return JWT token
     *
     * Process:
     * 1. Receive username and password from request body
     * 2. Validate credentials using Spring Security's authentication manager
     * 3. If valid, generate JWT token containing user info
     * 4. Return token and user details in response
     *
     * @param loginRequest Contains username and password
     * @return ResponseEntity with JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Authenticate user - throws exception if invalid credentials
        Authentication authentication = authenticateUserInternal(loginRequest);

        // Extract user details from authenticated principal
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        // Extract user roles from authorities
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)  // Convert GrantedAuthority to String
                .collect(Collectors.toList());

        // Create response with JWT token and user information
        return ResponseEntity.ok(new JwtResponse(jwtUtils.generateJwtToken(authentication),
                                                 userDetails.getId(),
                                                 userDetails.getUsername(),
                                                 userDetails.getEmail(),
                                                 roles));
    }

    /**
     * Internal method to authenticate user credentials
     * Uses Spring Security's authentication manager
     * @param loginRequest Contains username and password
     * @return Authentication object if credentials are valid
     */
    private Authentication authenticateUserInternal(LoginRequest loginRequest) {
        // Create authentication token with username and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                       loginRequest.getPassword()));

        // Set authentication in security context (for current request)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    /**
     * POST /api/auth/signup - Register a new user
     *
     * Process:
     * 1. Validate that username/email don't already exist
     * 2. Create new user with provided information
     * 3. Hash password before saving
     * 4. Assign appropriate roles (defaults to TESTER if none specified)
     * 5. Save user to database
     *
     * @param signUpRequest Contains username, email, password, and optional roles
     * @return ResponseEntity with success/error message
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()  // Return 400 Bad Request
                    .body("Error: Username is already taken!");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()  // Return 400 Bad Request
                    .body("Error: Email is already in use!");
        }

        // Create new user account with encoded password
        User user = new User(signUpRequest.getUsername(),
                            signUpRequest.getEmail(),
                            encoder.encode(signUpRequest.getPassword()));

        // Handle role assignment
        Set<String> strRoles = signUpRequest.getRole();  // Roles from request
        Set<Role> roles = new HashSet<>();  // Actual Role objects to assign

        if (strRoles == null) {
            // If no roles specified, assign default TESTER role
            Role userRole = roleRepository.findByName("TESTER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            // Assign specified roles
            strRoles.forEach(role -> {
                switch (role) {
                case "ADMIN":
                    Role adminRole = roleRepository.findByName("ADMIN")
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(adminRole);
                    break;
                case "QA":
                    Role qaRole = roleRepository.findByName("QA")
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(qaRole);
                    break;
                case "BA":
                    Role baRole = roleRepository.findByName("BA")
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(baRole);
                    break;
                default:
                    // Default to TESTER for unrecognized roles
                    Role testerRole = roleRepository.findByName("TESTER")
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(testerRole);
                }
            });
        }

        user.setRoles(roles);  // Assign roles to user
        userRepository.save(user);  // Save user to database

        return ResponseEntity.ok("User registered successfully!");
    }
}

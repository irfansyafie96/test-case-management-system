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
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        // Authenticate user - throws exception if invalid credentials
        Authentication authentication = authenticateUserInternal(loginRequest);

        // Extract user details from authenticated principal
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        // Extract user roles from authorities (strip "ROLE_" prefix for frontend)
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)  // Convert GrantedAuthority to String
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(Collectors.toList());

        // Generate JWT token
        String jwtToken = jwtUtils.generateJwtToken(authentication);

        // Create HttpOnly cookie for JWT token
        Cookie jwtCookie = new Cookie("JWT_TOKEN", jwtToken);
        jwtCookie.setHttpOnly(true);  // Prevent client-side JavaScript access
        jwtCookie.setSecure(false);   // Set to true in production with HTTPS
        jwtCookie.setPath("/");       // Make available for all paths
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
        jwtCookie.setAttribute("SameSite", "None");  // Allow cookie to be sent with PUT requests (non-safe methods)
        // Remove domain setting to work better with proxy configurations

        response.addCookie(jwtCookie);

        // Also set CSRF token after successful login
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
            csrfCookie.setHttpOnly(false);  // Must be false for JavaScript access
            csrfCookie.setSecure(false);    // Set to true in production with HTTPS
            csrfCookie.setPath("/");
            csrfCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
            csrfCookie.setAttribute("SameSite", "None");  // Allow cookie to be sent with PUT requests (non-safe methods)
            // Remove domain setting to work better with proxy configurations
            response.addCookie(csrfCookie);
        }

        // Create response with user information (no token in response body)
        return ResponseEntity.ok(new JwtResponse(null,  // Token removed from response body
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest, BindingResult bindingResult) {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return ResponseEntity
                    .badRequest()
                    .body("Validation errors: " + errors.toString());
        }

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
        user.setOrganization("default");  // Default organization
        userRepository.save(user);  // Save user to database

        return ResponseEntity.ok("User registered successfully!");
    }

    /**
     * POST /api/auth/logout - Clear JWT cookie to logout user
     * @param response HttpServletResponse to clear cookie
     * @return ResponseEntity with logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logoutUser(HttpServletResponse response) {
        // Clear JWT cookie
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);   // Set to true in production with HTTPS
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);  // Immediately expire cookie
        jwtCookie.setAttribute("SameSite", "None");  // Consistent with login endpoint

        response.addCookie(jwtCookie);

        // Return a proper JSON response that Angular expects
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "User logged out successfully!");
        return ResponseEntity.ok(responseMap);
    }

    /**
     * GET /api/auth/csrf - Trigger CSRF token generation
     * This endpoint exists to help frontend synchronize CSRF tokens after login
     * @return ResponseEntity indicating CSRF token refresh
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> refreshCsrfToken(HttpServletResponse response, HttpServletRequest request) {
        // This endpoint's main purpose is to trigger Spring Security's CSRF token generation
        // The actual CSRF token will be set in cookies automatically by Spring Security
        
        // Explicitly force CSRF token generation and ensure it's set in the response
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Ensure the CSRF token cookie is set with proper attributes
            Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
            csrfCookie.setHttpOnly(false);  // Must be false for JavaScript access
            csrfCookie.setSecure(false);    // Set to true in production with HTTPS
            csrfCookie.setPath("/");
            csrfCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
            csrfCookie.setAttribute("SameSite", "None");  // Consistent with login endpoint
            response.addCookie(csrfCookie);
        }

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "CSRF token refreshed");
        responseBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        if (csrfToken != null) {
            responseBody.put("tokenSet", "true");
        }

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/auth/check - Verify authentication is working
     * This endpoint can be used to verify that JWT token authentication is properly established
     * @return ResponseEntity with user authentication status
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAuth() {
        // This endpoint will return user info if authentication is valid
        // This allows the frontend to verify that the JWT token is working correctly

        // Get the authenticated user details from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            // User is authenticated, return basic user info as verification
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("user", authentication.getName());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } else {
            // User is not authenticated
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

    @Autowired
    com.yourproject.tcm.repository.OrganizationRepository organizationRepository;

    @Autowired
    com.yourproject.tcm.service.EmailService emailService;

    @Autowired
    com.yourproject.tcm.repository.EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private Environment environment;  // To check if running in production profile

    /**
     * POST /api/auth/otp - Generate and send OTP for organization registration
     * @param request Contains email address
     * @return ResponseEntity with success/error message
     */
    @PostMapping("/otp")
    public ResponseEntity<?> generateOtp(@Valid @RequestBody com.yourproject.tcm.model.dto.OtpGenerationRequest request) {
        // Check if email is already in use
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Save to DB (expires in 15 minutes)
        com.yourproject.tcm.model.EmailVerification verification = new com.yourproject.tcm.model.EmailVerification(
            request.getEmail(), 
            otp, 
            15
        );
        emailVerificationRepository.save(verification);

        // Send Email
        emailService.sendOtpEmail(request.getEmail(), otp);

        return ResponseEntity.ok("OTP sent to email!");
    }

    /**
     * POST /api/auth/register-org - Register a new organization with verified OTP
     * @param request Contains org details, admin details, and OTP
     * @return ResponseEntity with success/error message
     */
    @PostMapping("/register-org")
    public ResponseEntity<?> registerOrganization(@Valid @RequestBody com.yourproject.tcm.model.dto.OrganizationSignupRequest request) {
        // 1. Verify OTP
        com.yourproject.tcm.model.EmailVerification verification = emailVerificationRepository.findByEmail(request.getEmail())
            .orElse(null);

        if (verification == null) {
            return ResponseEntity.badRequest().body("Error: No OTP found for this email. Please request a new one.");
        }

        if (verification.isExpired()) {
            return ResponseEntity.badRequest().body("Error: OTP has expired. Please request a new one.");
        }

        if (!verification.getOtpCode().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Error: Invalid OTP.");
        }

        // 2. Validate Uniqueness
        if (organizationRepository.existsByName(request.getOrganizationName())) {
            return ResponseEntity.badRequest().body("Error: Organization name is already taken!");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }
        
        // (Email uniqueness checked at OTP generation, but good to double check or strictly lock)
        if (userRepository.existsByEmail(request.getEmail())) {
             return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        // 3. Create Organization
        com.yourproject.tcm.model.Organization org = new com.yourproject.tcm.model.Organization(request.getOrganizationName());
        org = organizationRepository.save(org);

        // 4. Create Admin User
        User user = new User(request.getUsername(),
                            request.getEmail(),
                            encoder.encode(request.getPassword()));
        
        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(adminRole);

        user.setRoles(roles);
        user.setOrganization(org);
        
        userRepository.save(user);

        // 5. Cleanup OTP
        emailVerificationRepository.delete(verification);

        return ResponseEntity.ok("Organization registered successfully! You can now login.");
    }

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
        jwtCookie.setHttpOnly(true);  // Prevent client-side JavaScript access (XSS protection)
        jwtCookie.setSecure(environment.acceptsProfiles("prod"));  // HTTPS only in production
        jwtCookie.setPath("/");       // Make available for all paths
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
        jwtCookie.setAttribute("SameSite", "Strict");  // Prevent CSRF attacks
        // Security Note: 
        // - HttpOnly: Prevents JavaScript from accessing cookies (XSS protection)
        // - Secure: Ensures cookies only sent over HTTPS (true in production)
        // - SameSite=Strict: Prevents cookies from being sent with cross-site requests (CSRF protection)

        response.addCookie(jwtCookie);

        // Also set CSRF token after successful login
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
            csrfCookie.setHttpOnly(false);  // Must be false for JavaScript access (Angular needs it)
            csrfCookie.setSecure(environment.acceptsProfiles("prod"));  // HTTPS only in production
            csrfCookie.setPath("/");
            csrfCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
            csrfCookie.setAttribute("SameSite", "Lax");  // Lax allows cookies on same-site GET requests
            // Security Note:
            // - HttpOnly=false: Required for Angular to read CSRF token
            // - Secure: Ensures cookies only sent over HTTPS (true in production)
            // - SameSite=Lax: Allows cookies on same-site GET requests (needed for navigation)
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
        
        // Handle organization assignment (Legacy support: default to "default" org)
        com.yourproject.tcm.model.Organization org = organizationRepository.findByName("default")
            .orElseGet(() -> organizationRepository.save(new com.yourproject.tcm.model.Organization("default")));
        user.setOrganization(org);
        
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
        // Note: Using Lax (default) which works properly with HTTP

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
            // Note: Using Lax (default) which works properly with HTTP
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

    /**
     * GET /api/auth/users - Get all non-admin users (QA/BA/TESTER) for admin dashboard filter
     * Requires ADMIN role
     * @return ResponseEntity with list of non-admin users filtered by organization
     */
    @GetMapping("/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.yourproject.tcm.model.dto.UserDTO>> getAllNonAdminUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Error: Current user not found."));
        
        List<User> users = userRepository.findAllNonAdminUsers(currentUser.getOrganization());
        
        List<com.yourproject.tcm.model.dto.UserDTO> userDTOs = users.stream()
            .map(user -> new com.yourproject.tcm.model.dto.UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getOrganizationName(),
                user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(userDTOs);
    }
}

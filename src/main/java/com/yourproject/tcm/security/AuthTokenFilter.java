package com.yourproject.tcm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AuthTokenFilter - JWT Authentication Filter
 *
 * This filter runs for every incoming HTTP request to:
 * 1. Extract JWT token from Authorization header
 * 2. Validate the token (format, expiration, signature)
 * 3. Load user details from database
 * 4. Set authentication in security context for the current request
 *
 * The filter runs BEFORE the main controller method executes,
 * ensuring authentication is checked for all protected endpoints.
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {  // Ensures filter runs only once per request
    @Autowired
    private JwtUtils jwtUtils;  // Utility class for JWT operations

    @Autowired
    private UserDetailsService userDetailsService;  // Service to load user details from database

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);  // For logging

    /**
     * Main filter method that processes each incoming request
     * Checks for JWT token and validates it
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Don't process authentication for logout requests since the token is being cleared
            String requestURI = request.getRequestURI();
            String requestMethod = request.getMethod();
            logger.debug("Processing {} request for URI: {}", requestMethod, requestURI);
            
            // Only skip authentication for logout endpoint
            if ("/api/auth/logout".equals(requestURI)) {
                logger.debug("Skipping authentication for logout request");
                // For logout, just continue with the filter chain without setting authentication
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JWT token from request header/cookie
            String jwt = parseJwt(request);
            logger.debug("Extracted JWT token: {}", jwt != null ? "found" : "not found");

            // Debug: Log all cookies for troubleshooting
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    logger.debug("Cookie found: {} = {}", cookie.getName(), cookie.getValue());
                }
            } else {
                logger.debug("No cookies found in request");
            }

            // If token exists and is valid, set up authentication
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                logger.debug("JWT token is valid, setting up authentication for user");
                // Extract username from token
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                logger.debug("Username from JWT: {}", username);

                // Load user details from database based on username
                // This calls the loadUserByUsername method in UserDetailsServiceImpl
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create authentication object with user details and authorities (roles)
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails,
                                                           null,  // No credentials needed since we already validated token
                                                           userDetails.getAuthorities());  // User's roles/permissions

                // Set request details in authentication object
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context for current request
                // This makes user info available throughout the request processing
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set successfully for user: {}", username);
            } else {
                logger.debug("JWT token validation failed or token is null");
                // For DELETE requests on /api/projects/*, log extra info for debugging
                if ("DELETE".equals(requestMethod) && requestURI.startsWith("/api/projects/")) {
                    logger.error("DELETE request to {} without valid authentication!", requestURI);
                }
            }
        } catch (Exception e) {
            // Log any errors during authentication setup
            logger.error("Cannot set user authentication: {}", e.getMessage());
            logger.error("Exception details:", e);
        }

        // Continue with the filter chain (pass request to next filter/controller)
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from HttpOnly cookie or Authorization header (fallback)
     * Primary: HttpOnly cookie (more secure)
     * Fallback: Authorization header "Bearer <token>" (backward compatibility)
     * @param request HTTP request containing cookie or header
     * @return JWT token string, or null if not found/invalid format
     */
    private String parseJwt(HttpServletRequest request) {
        // Log all cookies for debugging DELETE requests
        String requestMethod = request.getMethod();
        String requestURI = request.getRequestURI();
        if ("DELETE".equals(requestMethod) && requestURI.startsWith("/api/projects/")) {
            logger.debug("DELETE request to {} - checking cookies", requestURI);
            if (request.getCookies() != null) {
                logger.debug("Found {} cookies", request.getCookies().length);
                for (Cookie cookie : request.getCookies()) {
                    logger.debug("Cookie: {} = {}", cookie.getName(), cookie.getValue() != null ? "[present]" : "[null]");
                }
            } else {
                logger.debug("No cookies found in request");
            }
        }
        
        // First try to get JWT from HttpOnly cookie (more secure)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback to Authorization header for backward compatibility
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // Return null if no valid token found
        return null;
    }
}

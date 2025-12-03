package com.yourproject.tcm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
            // Extract JWT token from request header
            String jwt = parseJwt(request);

            // If token exists and is valid, set up authentication
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Extract username from token
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

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
            }
        } catch (Exception e) {
            // Log any errors during authentication setup
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Continue with the filter chain (pass request to next filter/controller)
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     * @param request HTTP request containing Authorization header
     * @return JWT token string, or null if not found/invalid format
     */
    private String parseJwt(HttpServletRequest request) {
        // Get Authorization header value
        String headerAuth = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer " prefix
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // Extract and return the actual token (remove "Bearer " prefix)
            return headerAuth.substring(7);
        }

        // Return null if no valid Authorization header found
        return null;
    }
}

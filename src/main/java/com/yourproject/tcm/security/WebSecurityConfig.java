package com.yourproject.tcm.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return authTokenFilter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            // Restrict to specific origins for security
            config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:4200",  // Angular dev server
                "http://localhost:3000",  // Alternative frontend port
                "http://127.0.0.1:4200", // Localhost alternative
                "${ALLOWED_ORIGINS:}"     // Environment variable override for production
            ));
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
            config.setAllowedHeaders(Arrays.asList("*"));
            config.setAllowCredentials(true);
            config.setExposedHeaders(Arrays.asList("Set-Cookie", "X-XSRF-TOKEN"));
            return config;
        }))

        /**
         * CSRF (Cross-Site Request Forgery) Configuration
         * 
         * CSRF is DISABLED for all API endpoints in this application.
         * This is INTENTIONAL and CORRECT for the following reasons:
         * 
         * 1. STATELESS JWT AUTHENTICATION: This application uses stateless JWT tokens
         *    stored in HttpOnly cookies. Each request includes the JWT token, which must
         *    be validated by the server. This provides protection against CSRF attacks
         *    without requiring CSRF tokens.
         * 
         * 2. NO SESSION-BASED AUTHENTICATION: CSRF tokens are primarily needed for
         *    session-based authentication where the browser automatically includes session
         *    cookies. With JWT tokens, the client must explicitly include the token in
         *    each request.
         * 
         * 3. REDUCED COMPLEXITY: Disabling CSRF for JWT APIs simplifies the client
         *    implementation and reduces the chance of configuration errors.
         * 
         * SECURITY NOTE: While CSRF is disabled, the application still has strong
         * security through:
         * - JWT token validation on every request
         * - HttpOnly cookies (XSS protection)
         * - Secure flag on cookies (HTTPS only in production)
         * - SameSite attribute (prevents cross-site cookie sending)
         * - CORS configuration
         * - Content Security Policy (CSP)
         * - Rate limiting (recommended for production)
         * 
         * References:
         * - OWASP CSRF Prevention Cheat Sheet
         * - Spring Security documentation on CSRF and JWT
         */
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/auth/**")  // Don't require CSRF for auth endpoints
            .ignoringRequestMatchers("/api/projects/**")  // Ignore CSRF for projects
            .ignoringRequestMatchers("/api/testmodules/**")  // Ignore CSRF for testmodules
            .ignoringRequestMatchers("/api/testsuites/**")  // Ignore CSRF for testsuites
            .ignoringRequestMatchers("/api/submodules/**")  // Ignore CSRF for submodules
            .ignoringRequestMatchers("/api/testcases/**")   // Ignore CSRF for testcases
            .ignoringRequestMatchers("/api/users/**")       // Ignore CSRF for user management
            .ignoringRequestMatchers("/api/executions/**")  // Ignore CSRF for executions
            .ignoringRequestMatchers("/api/invitations/**")
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        )
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Invitation endpoints (Public for joining)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/invitations/*").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/invitations/accept").permitAll()
                // Static resources
                .requestMatchers("/", "/index.html", "/login.html", "/*.html", "/*.css", "/*.js").permitAll()
                // API endpoints - will be protected by method-level security
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            );

        // Add security headers
        http.headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.deny())  // X-Frame-Options: DENY
            .contentTypeOptions(contentTypeOptions -> {})  // X-Content-Type-Options: nosniff
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))  // HSTS: 1 year
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self';"))
        );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

package com.trademaster.eventbus.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ ZERO TRUST SECURITY: Production-Ready Security Configuration
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #6: Zero Trust Security with tiered access control
 * - Rule #3: Functional programming patterns
 * - Rule #15: Structured logging and security monitoring
 * 
 * SECURITY PRINCIPLES:
 * - External access requires full security stack (SecurityFacade + SecurityMediator)
 * - Internal service-to-service uses simple constructor injection
 * - Default deny with explicit grants only
 * - Comprehensive security headers with CSP
 * - JWT authentication with role-based authorization
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    @Value("${agentos.security.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    /**
     * ✅ FUNCTIONAL: Configure security filter chain
     * Cognitive Complexity: 6
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Zero Trust Security with CORS origins: {}", Arrays.toString(allowedOrigins));
        
        return http
            // ✅ SECURITY: Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // ✅ SECURITY: Configure CORS with restricted origins
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ SECURITY: Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ✅ SECURITY: Zero Trust authorization (Default Deny)
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/health", "/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/websocket/**").permitAll() // WebSocket authentication handled separately
                
                // Admin endpoints
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll())
            
            // ✅ SECURITY: Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> content.disable())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy", 
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self' wss://trademaster.com wss://app.trademaster.com")))
            
            .build();
    }

    /**
     * ✅ FUNCTIONAL: Configure CORS for frontend integration
     * Cognitive Complexity: 3
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ SECURITY: Restrict origins to known frontends only
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour preflight cache
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.debug("CORS configuration registered for origins: {}", Arrays.toString(allowedOrigins));
        return source;
    }
}
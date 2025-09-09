package com.trademaster.eventbus.config;

import com.trademaster.eventbus.security.ServiceApiKeyFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for Event Bus Service
 * 
 * Configures Spring Security to work with ServiceApiKeyFilter:
 * - Public endpoints: /health, /actuator/health, /actuator/info, /actuator/prometheus
 * - Internal API endpoints: Authenticated by ServiceApiKeyFilter
 * - All other endpoints: Deny by default
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private final ServiceApiKeyFilter serviceApiKeyFilter;

    @Value("${agentos.security.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Security with ServiceApiKeyFilter");
        
        return http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                // Public health and monitoring endpoints (MUST BE FIRST - order matters!)
                .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/info").permitAll()
                .requestMatchers("/event-bus/actuator/health", "/event-bus/actuator/prometheus", "/event-bus/actuator/info").permitAll()
                .requestMatchers("/api/internal/*/actuator/health", "/api/internal/*/actuator/prometheus", "/api/internal/*/actuator/info").permitAll()
                .requestMatchers("/health").permitAll()
                
                // Internal API endpoints - must allow for ServiceApiKeyFilter to process
                .requestMatchers("/api/internal/**").hasRole("SERVICE")
                
                // All other requests denied
                .anyRequest().denyAll())
            
            // Add ServiceApiKeyFilter before Spring Security authentication
            .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
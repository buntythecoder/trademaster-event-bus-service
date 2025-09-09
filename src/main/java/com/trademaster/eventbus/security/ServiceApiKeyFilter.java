package com.trademaster.eventbus.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Service API Key Authentication Filter for Event Bus Service
 * 
 * Provides authentication for service-to-service communication using API keys.
 * This filter runs before JWT authentication and handles internal service calls.
 * 
 * Security Features:
 * - API key validation for internal services
 * - Request path filtering (only /api/internal/*)
 * - Audit logging for service authentication
 * - Fail-safe authentication bypass for health checks
 * 
 * Integration Pattern:
 * - External access uses SecurityFacade + SecurityMediator (Zero Trust)
 * - Internal service-to-service uses this API key filter (lightweight)
 * - Service boundary security with proper role assignment
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Order(-100) // Run very early, before Spring Security filters
@Slf4j
public class ServiceApiKeyFilter implements Filter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String KONG_CONSUMER_CUSTOM_ID_HEADER = "X-Consumer-Custom-ID";
    private static final String INTERNAL_API_PATH = "/api/internal/";
    
    @Value("${trademaster.security.service.api-key:}")
    private String masterServiceApiKey;
    
    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        String servletPath = httpRequest.getServletPath();
        
        // Only process internal API requests (handle context path correctly)
        boolean isInternalRequest = requestPath.contains(INTERNAL_API_PATH) || 
                                   servletPath.startsWith(INTERNAL_API_PATH);
        
        // Skip public health endpoints (actuator endpoints only, not internal API health)
        boolean isPublicHealthEndpoint = (requestPath.equals("/health") || 
                                        requestPath.equals("/actuator/health") ||
                                        servletPath.equals("/health") ||
                                        servletPath.equals("/actuator/health")) && 
                                       !requestPath.contains(INTERNAL_API_PATH);
        
        if (!isInternalRequest || isPublicHealthEndpoint) {
            chain.doFilter(request, response);
            return;
        }
        
        // Skip authentication if disabled (for local development)
        if (!serviceAuthEnabled) {
            log.warn("Service authentication is DISABLED - allowing internal API access");
            setServiceAuthentication("development-service");
            chain.doFilter(request, response);
            return;
        }
        
        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        String kongConsumerId = httpRequest.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = httpRequest.getHeader(KONG_CONSUMER_USERNAME_HEADER);
        String kongConsumerCustomId = httpRequest.getHeader(KONG_CONSUMER_CUSTOM_ID_HEADER);
        
        // Debug: Log all headers starting with X-
        log.info("ServiceApiKeyFilter Debug - All X- headers:");
        java.util.Collections.list(httpRequest.getHeaderNames()).stream()
            .filter(name -> name.toLowerCase().startsWith("x-"))
            .forEach(name -> log.info("  {}: {}", name, httpRequest.getHeader(name)));
        
        log.info("ServiceApiKeyFilter: API Key: {}, Kong Consumer ID: {}, Kong Consumer Username: {}, Custom ID: {}", 
                 apiKey != null ? "Present" : "Missing", 
                 kongConsumerId != null ? "Present" : "Missing",
                 kongConsumerUsername != null ? kongConsumerUsername : "Missing",
                 kongConsumerCustomId != null ? kongConsumerCustomId : "Missing");
        
        // If Kong consumer headers are present, Kong has already validated the API key
        if (StringUtils.hasText(kongConsumerId) && StringUtils.hasText(kongConsumerUsername)) {
            log.info("ServiceApiKeyFilter: Kong validated consumer '{}' (ID: {}), granting SERVICE access", 
                     kongConsumerUsername, kongConsumerId);
            setServiceAuthentication(kongConsumerUsername);
            chain.doFilter(request, response);
            return;
        }
        
        // If no Kong headers, check if we have fallback validation enabled
        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing API key and no Kong consumer headers for internal service request: {} from {}", 
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Missing service authentication");
            return;
        }
        
        log.warn("ServiceApiKeyFilter: No Kong consumer headers, falling back to direct validation");
        
        // Fallback: validate API key directly (for direct service calls)
        if (!isValidServiceApiKey(apiKey, "direct-service")) {
            log.error("Invalid API key for direct service request: {} from {}", 
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Invalid service credentials");
            return;
        }
        
        // Set service authentication in security context
        setServiceAuthentication("direct-service");
        
        log.info("Service authentication successful: direct-service accessing {}", requestPath);
        
        chain.doFilter(request, response);
    }
    
    /**
     * Validate service API key
     */
    private boolean isValidServiceApiKey(String apiKey, String serviceId) {
        // For now, use master API key for all services
        // In production, you might have service-specific keys stored in database/vault
        if (!StringUtils.hasText(masterServiceApiKey)) {
            log.error("Master service API key not configured");
            return false;
        }
        
        // Simple validation - in production, use more sophisticated validation
        boolean isValid = masterServiceApiKey.equals(apiKey) && isKnownService(serviceId);
        
        if (!isValid) {
            log.error("API key validation failed for service: {}", serviceId);
        }
        
        return isValid;
    }
    
    /**
     * Check if service ID is in our known services list
     */
    private boolean isKnownService(String serviceId) {
        return List.of(
            "trading-service",
            "broker-auth-service", 
            "portfolio-service",
            "notification-service",
            "risk-service",
            "user-service",
            "audit-service"
        ).contains(serviceId);
    }
    
    /**
     * Set service authentication in Spring Security context
     */
    private void setServiceAuthentication(String serviceId) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_SERVICE"),
            new SimpleGrantedAuthority("ROLE_INTERNAL")
        );
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(serviceId, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"SERVICE_AUTHENTICATION_FAILED\",\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis()));
    }
}
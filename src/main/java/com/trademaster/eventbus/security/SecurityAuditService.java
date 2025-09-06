package com.trademaster.eventbus.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Single Responsibility: Security Event Auditing
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: SOLID Principles - Single Responsibility for security auditing
 * - Rule #3: Functional Programming - No if-else, pattern matching
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total
 * - Rule #12: Virtual Threads for all async operations
 * - Rule #15: Structured logging with correlation IDs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {
    
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * ✅ FUNCTIONAL: Log security event asynchronously
     */
    public CompletableFuture<Void> logSecurityEventAsync(String userId, String operation, 
                                                         String status, Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> 
            logSecurityEvent(userId, operation, status, metadata), virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Log security event with structured logging
     */
    public void logSecurityEvent(String userId, String operation, String status, Map<String, Object> metadata) {
        AuditEvent auditEvent = new AuditEvent(
            generateAuditId(),
            userId,
            operation,
            status,
            Instant.now(),
            metadata
        );
        
        logAuditEvent(auditEvent);
    }
    
    /**
     * ✅ FUNCTIONAL: Log authentication events
     */
    public void logAuthenticationEvent(String userId, String tokenId, AuthenticationEventType eventType, 
                                      Map<String, Object> context) {
        Map<String, Object> authMetadata = Map.of(
            "event_type", eventType.toString(),
            "token_id", tokenId,
            "context", context
        );
        
        String status = switch (eventType) {
            case LOGIN_SUCCESS -> "AUTHENTICATION_SUCCESS";
            case LOGIN_FAILURE -> "AUTHENTICATION_FAILURE";
            case TOKEN_VALIDATION_SUCCESS -> "TOKEN_VALIDATION_SUCCESS";
            case TOKEN_VALIDATION_FAILURE -> "TOKEN_VALIDATION_FAILURE";
            case LOGOUT -> "LOGOUT_SUCCESS";
        };
        
        logSecurityEvent(userId, "AUTHENTICATION", status, authMetadata);
    }
    
    /**
     * ✅ FUNCTIONAL: Log authorization events
     */
    public void logAuthorizationEvent(String userId, String resource, String operation, 
                                     boolean authorized, String reason, Map<String, Object> context) {
        Map<String, Object> authzMetadata = Map.of(
            "resource", resource,
            "operation", operation,
            "authorized", authorized,
            "reason", reason,
            "context", context
        );
        
        String status = authorized ? "AUTHORIZATION_GRANTED" : "AUTHORIZATION_DENIED";
        logSecurityEvent(userId, "AUTHORIZATION", status, authzMetadata);
    }
    
    /**
     * ✅ FUNCTIONAL: Log risk assessment events
     */
    public void logRiskAssessmentEvent(String userId, String riskLevel, String assessment, 
                                      Map<String, Object> riskFactors) {
        Map<String, Object> riskMetadata = Map.of(
            "risk_level", riskLevel,
            "assessment", assessment,
            "risk_factors", riskFactors
        );
        
        logSecurityEvent(userId, "RISK_ASSESSMENT", "RISK_ASSESSED", riskMetadata);
    }
    
    // ✅ PRIVATE HELPERS: Functional audit processing
    
    private void logAuditEvent(AuditEvent event) {
        switch (event.status()) {
            case String status when status.contains("SUCCESS") -> 
                log.info("Security audit: userId={}, operation={}, status={}, timestamp={}, metadata={}", 
                    event.userId(), event.operation(), event.status(), event.timestamp(), event.metadata());
            
            case String status when status.contains("FAILURE") || status.contains("DENIED") -> 
                log.warn("Security violation: userId={}, operation={}, status={}, timestamp={}, metadata={}", 
                    event.userId(), event.operation(), event.status(), event.timestamp(), event.metadata());
            
            case String status when status.contains("ERROR") -> 
                log.error("Security error: userId={}, operation={}, status={}, timestamp={}, metadata={}", 
                    event.userId(), event.operation(), event.status(), event.timestamp(), event.metadata());
            
            default -> 
                log.info("Security event: userId={}, operation={}, status={}, timestamp={}, metadata={}", 
                    event.userId(), event.operation(), event.status(), event.timestamp(), event.metadata());
        }
    }
    
    private String generateAuditId() {
        return "audit-" + System.nanoTime() + "-" + Thread.currentThread().threadId();
    }
    
    // ✅ ENUMS: Type-safe event classification
    
    public enum AuthenticationEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        TOKEN_VALIDATION_SUCCESS,
        TOKEN_VALIDATION_FAILURE,
        LOGOUT
    }
    
    // ✅ IMMUTABLE: Audit event record
    
    public record AuditEvent(
        String auditId,
        String userId,
        String operation,
        String status,
        Instant timestamp,
        Map<String, Object> metadata
    ) {}
}
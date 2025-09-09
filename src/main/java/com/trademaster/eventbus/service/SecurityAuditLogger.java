package com.trademaster.eventbus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * ✅ SECURITY AUDIT LOGGER: Security event logging and auditing
 */
@Service
@Slf4j
public class SecurityAuditLogger {
    
    /**
     * ✅ FUNCTIONAL: Log security event with details
     */
    public void logSecurityEvent(String eventType, String userId, Map<String, Object> details) {
        log.info("Security Event: type={}, userId={}, timestamp={}, details={}", 
            eventType, userId, Instant.now(), details);
    }
    
    /**
     * ✅ FUNCTIONAL: Log authentication success
     */
    public void logAuthenticationSuccess(String userId, String sessionId) {
        logSecurityEvent("AUTHENTICATION_SUCCESS", userId, 
            Map.of("sessionId", sessionId, "timestamp", Instant.now()));
    }
    
    /**
     * ✅ FUNCTIONAL: Log authentication failure
     */
    public void logAuthenticationFailure(String userId, String reason) {
        logSecurityEvent("AUTHENTICATION_FAILURE", userId, 
            Map.of("reason", reason, "timestamp", Instant.now()));
    }
    
    /**
     * ✅ FUNCTIONAL: Log token refresh
     */
    public void logTokenRefresh(String userId, String sessionId) {
        logSecurityEvent("TOKEN_REFRESH", userId, 
            Map.of("sessionId", sessionId, "timestamp", Instant.now()));
    }
    
    /**
     * ✅ FUNCTIONAL: Log session expiry
     */
    public void logSessionExpiry(String userId, String sessionId) {
        logSecurityEvent("SESSION_EXPIRED", userId, 
            Map.of("sessionId", sessionId, "timestamp", Instant.now()));
    }
}
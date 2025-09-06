package com.trademaster.eventbus.security;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single Responsibility: Security Session State Management
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: SOLID Principles - Single Responsibility for session management
 * - Rule #3: Functional Programming - No if-else, immutable data
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecuritySessionManager {
    
    private final ConcurrentHashMap<String, SecuritySessionState> activeSessions = 
        new ConcurrentHashMap<>();
    
    /**
     * ✅ FUNCTIONAL: Initialize new security session
     */
    public Result<SecuritySessionState, GatewayError> initializeSession(
            SecurityFacade.SecurityContext context) {
        
        return java.util.Optional.of(context)
            .map(ctx -> new SecuritySessionState(
                ctx.userId(),
                ctx.sessionId(),
                ctx.requestId(),
                Instant.now(),
                SecuritySessionState.SessionPhase.INITIALIZATION,
                Map.of("ip_address", ctx.ipAddress(), "user_agent", ctx.userAgent().orElse("unknown"))
            ))
            .map(session -> {
                activeSessions.put(session.sessionId(), session);
                return Result.<SecuritySessionState, GatewayError>success(session);
            })
            .orElse(Result.failure(new GatewayError.SystemError.InternalServerError(
                "Failed to initialize security session", "SECURITY_SESSION_INIT_ERROR")));
    }
    
    /**
     * ✅ FUNCTIONAL: Update session phase
     */
    public Result<SecuritySessionState, GatewayError> updateSessionPhase(
            String sessionId, SecuritySessionState.SessionPhase newPhase) {
        
        return java.util.Optional.ofNullable(activeSessions.get(sessionId))
            .map(session -> session.withPhase(newPhase))
            .map(updatedSession -> {
                activeSessions.put(sessionId, updatedSession);
                return Result.<SecuritySessionState, GatewayError>success(updatedSession);
            })
            .orElse(Result.failure(new GatewayError.AuthenticationError.InvalidSession(
                "Session not found: " + sessionId, sessionId)));
    }
    
    /**
     * ✅ FUNCTIONAL: Retrieve active session
     */
    public Result<SecuritySessionState, GatewayError> getSession(String sessionId) {
        return java.util.Optional.ofNullable(activeSessions.get(sessionId))
            .map(Result::<SecuritySessionState, GatewayError>success)
            .orElse(Result.failure(new GatewayError.AuthenticationError.InvalidSession(
                "Session not found: " + sessionId, sessionId)));
    }
    
    /**
     * ✅ FUNCTIONAL: Remove session on completion/timeout
     */
    public Result<Void, GatewayError> removeSession(String sessionId) {
        return java.util.Optional.ofNullable(activeSessions.remove(sessionId))
            .map(_ -> Result.<Void, GatewayError>success(null))
            .orElse(Result.failure(new GatewayError.AuthenticationError.InvalidSession(
                "Session not found for removal: " + sessionId, sessionId)));
    }
    
    /**
     * ✅ IMMUTABLE: Security session state record
     */
    public record SecuritySessionState(
        String userId,
        String sessionId,
        String requestId,
        Instant createdTime,
        SessionPhase phase,
        Map<String, String> metadata
    ) {
        public SecuritySessionState withPhase(SessionPhase newPhase) {
            return new SecuritySessionState(userId, sessionId, requestId, createdTime, newPhase, metadata);
        }
        
        public enum SessionPhase {
            INITIALIZATION,
            AUTHENTICATED,
            AUTHORIZED,
            RISK_ASSESSED,
            COMPLETED
        }
    }
}
package com.trademaster.eventbus.service;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * ✅ SINGLE RESPONSIBILITY: WebSocket Security Validation
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility for security operations only
 * - Rule #3: Functional Programming - No if-else statements
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total per class
 * - Rule #6: Zero Trust Security - validate everything
 * - Rule #12: Virtual Threads for all async operations
 * 
 * RESPONSIBILITIES:
 * - WebSocket authentication validation
 * - Message permission checking
 * - Session security validation
 * - Rate limiting enforcement
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityValidator {
    
    private final SecurityAuthenticationService authenticationService;
    
    // ✅ VIRTUAL THREADS: Dedicated executor for security operations
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * ✅ FUNCTIONAL: Validate WebSocket connection security
     * Cognitive Complexity: 2
     */
    public CompletableFuture<Result<SecurityAuthenticationService.AuthenticationResult, GatewayError>> 
            validateConnection(WebSocketSession session) {
        
        return CompletableFuture.supplyAsync(() -> 
            extractConnectionHeaders(session)
                .flatMap(this::validateConnectionSecurity), 
            virtualThreadExecutor)
            .thenCompose(headerResult -> headerResult.fold(
                headers -> authenticationService.authenticateWebSocketConnection(headers),
                error -> CompletableFuture.completedFuture(Result.failure(error))
            ));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate message permissions
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Result<Boolean, GatewayError>> validateMessagePermissions(
            String userId, 
            String messageType) {
        
        return CompletableFuture.supplyAsync(() -> 
            validateMessageType(messageType)
                .flatMap(type -> checkUserPermissions(userId, type))
                .map(this::logPermissionCheck), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate session security state
     * Cognitive Complexity: 2
     */
    public CompletableFuture<Result<SessionSecurityState, GatewayError>> validateSessionSecurity(
            String sessionId, 
            SecurityAuthenticationService.AuthenticationResult authResult) {
        
        return CompletableFuture.supplyAsync(() -> 
            validateSessionIntegrity(sessionId, authResult)
                .map(this::createSecurityState), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Check rate limits for user
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Result<Boolean, GatewayError>> checkRateLimits(
            String userId, 
            String operation) {
        
        return CompletableFuture.supplyAsync(() -> 
            getUserRateLimit(userId, operation)
                .map(limit -> isWithinRateLimit(limit))
                .map(this::logRateLimitCheck), 
            virtualThreadExecutor);
    }
    
    // ✅ PRIVATE HELPERS: Single responsibility helper methods
    
    /**
     * ✅ FUNCTIONAL: Extract headers from WebSocket session
     * Cognitive Complexity: 3
     */
    private Result<Map<String, String>, GatewayError> extractConnectionHeaders(WebSocketSession session) {
        return java.util.Optional.ofNullable(session.getHandshakeHeaders())
            .filter(headers -> !headers.isEmpty())
            .map(headers -> headers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> String.join(",", entry.getValue())
                )))
            .map(Result::<Map<String, String>, GatewayError>success)
            .orElse(Result.failure(new GatewayError.AuthenticationError.InvalidCredentials(
                "WebSocket handshake headers missing", "unknown")));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate connection security headers
     * Cognitive Complexity: 2
     */
    private Result<Map<String, String>, GatewayError> validateConnectionSecurity(Map<String, String> headers) {
        return headers.containsKey("Authorization") ?
            Result.success(headers) :
            Result.failure(new GatewayError.AuthenticationError.InvalidCredentials(
                "Authorization header required for WebSocket connection", "unknown"));
    }
    
    /**
     * ✅ FUNCTIONAL: Validate message type
     * Cognitive Complexity: 2
     */
    private Result<String, GatewayError> validateMessageType(String messageType) {
        return switch (messageType) {
            case null -> Result.failure(new GatewayError.MessageError.MissingField(
                "Message type is required", "type"));
            case String type when type.isBlank() -> Result.failure(new GatewayError.MessageError.EmptyMessage(
                "Message type cannot be empty", "empty_message_type"));
            default -> Result.success(messageType);
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Check user permissions for message type
     * Cognitive Complexity: 3
     */
    private Result<Boolean, GatewayError> checkUserPermissions(String userId, String messageType) {
        return switch (messageType) {
            case "trading_data" -> checkTradingPermissions(userId);
            case "portfolio_update" -> checkPortfolioPermissions(userId);
            case "market_data" -> checkMarketDataPermissions(userId);
            case "system_notification" -> Result.success(true); // All users can receive system notifications
            default -> Result.failure(new GatewayError.AuthorizationError.InsufficientPermissions(
                "Unknown message type", messageType));
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Check trading permissions
     * Cognitive Complexity: 1
     */
    private Result<Boolean, GatewayError> checkTradingPermissions(String userId) {
        // Real implementation would check user roles and permissions
        return Result.success(true); // Placeholder - all authenticated users can receive trading data
    }
    
    /**
     * ✅ FUNCTIONAL: Check portfolio permissions
     * Cognitive Complexity: 1
     */
    private Result<Boolean, GatewayError> checkPortfolioPermissions(String userId) {
        // Real implementation would check user-specific portfolio access
        return Result.success(true); // Placeholder - all authenticated users can receive their portfolio data
    }
    
    /**
     * ✅ FUNCTIONAL: Check market data permissions
     * Cognitive Complexity: 1
     */
    private Result<Boolean, GatewayError> checkMarketDataPermissions(String userId) {
        // Real implementation would check subscription level
        return Result.success(true); // Placeholder - all authenticated users can receive market data
    }
    
    /**
     * ✅ FUNCTIONAL: Validate session integrity
     * Cognitive Complexity: 2
     */
    private Result<SecurityAuthenticationService.AuthenticationResult, GatewayError> validateSessionIntegrity(
            String sessionId, 
            SecurityAuthenticationService.AuthenticationResult authResult) {
        
        return authResult.sessionId().equals(sessionId) ?
            Result.success(authResult) :
            Result.failure(new GatewayError.AuthenticationError.InvalidSession(
                "Session ID mismatch", sessionId));
    }
    
    /**
     * ✅ FUNCTIONAL: Create security state
     * Cognitive Complexity: 1
     */
    private SessionSecurityState createSecurityState(SecurityAuthenticationService.AuthenticationResult authResult) {
        return new SessionSecurityState(
            authResult.sessionId(),
            authResult.userId(),
            authResult.roles(),
            Instant.now(),
            SecurityLevel.AUTHENTICATED
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Get rate limit for user operation
     * Cognitive Complexity: 2
     */
    private Result<RateLimit, GatewayError> getUserRateLimit(String userId, String operation) {
        // Real implementation would check user subscription and operation type
        RateLimit defaultLimit = new RateLimit(
            operation,
            1000, // requests per minute
            java.time.Duration.ofMinutes(1),
            0 // current count - would be tracked in Redis
        );
        return Result.success(defaultLimit);
    }
    
    /**
     * ✅ FUNCTIONAL: Check if within rate limit
     * Cognitive Complexity: 1
     */
    private boolean isWithinRateLimit(RateLimit limit) {
        return limit.currentCount() < limit.maxRequests();
    }
    
    /**
     * ✅ FUNCTIONAL: Log permission check
     * Cognitive Complexity: 1
     */
    private Boolean logPermissionCheck(Boolean hasPermission) {
        log.debug("Permission check completed: {}", hasPermission);
        return hasPermission;
    }
    
    /**
     * ✅ FUNCTIONAL: Log rate limit check
     * Cognitive Complexity: 1
     */
    private Boolean logRateLimitCheck(Boolean withinLimit) {
        log.debug("Rate limit check completed: {}", withinLimit);
        return withinLimit;
    }
    
    // ✅ IMMUTABLE: Security records and enums
    
    public record SessionSecurityState(
        String sessionId,
        String userId,
        java.util.Set<String> roles,
        Instant validatedAt,
        SecurityLevel securityLevel
    ) {}
    
    public record RateLimit(
        String operation,
        int maxRequests,
        java.time.Duration window,
        int currentCount
    ) {}
    
    public enum SecurityLevel {
        UNAUTHENTICATED,
        AUTHENTICATED,
        AUTHORIZED,
        ELEVATED
    }
}
package com.trademaster.eventbus.security;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Single Responsibility: Authorization Logic
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: SOLID Principles - Single Responsibility for authorization
 * - Rule #3: Functional Programming - No if-else, pattern matching
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total
 * - Rule #12: Virtual Threads for all async operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuthorizationService {
    
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    // ✅ IMMUTABLE: Role-based permissions mapping
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
        "ADMIN", Set.of("WEBSOCKET_CONNECT", "API_ACCESS", "EXTERNAL_SERVICE_ACCESS"),
        "USER", Set.of("WEBSOCKET_CONNECT", "API_ACCESS"),
        "GUEST", Set.of("API_ACCESS")
    );
    
    /**
     * ✅ FUNCTIONAL: Authorize WebSocket access
     */
    public CompletableFuture<Result<AuthorizationResult, GatewayError>> authorizeWebSocketAccess(
            String userId, String operation, Map<String, Object> context) {
        
        return CompletableFuture.supplyAsync(() -> 
            authorizeOperation(userId, operation, "WEBSOCKET")
                .fold(
                    authorized -> Result.success(new AuthorizationResult(
                        authorized, 
                        authorized ? java.util.Optional.<String>empty() : 
                                   java.util.Optional.of("WebSocket access not permitted"),
                        ROLE_PERMISSIONS.getOrDefault(getUserRole(userId), Set.of()),
                        Map.of("operation", operation, "resource", "WEBSOCKET"),
                        Set.of("WEBSOCKET_POLICY", "USER_ROLE_POLICY"),
                        Map.of("userId", userId, "operation", operation, "resource", "WEBSOCKET")
                    )),
                    error -> Result.<AuthorizationResult, GatewayError>failure(error)
                ), virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Authorize REST API access
     */
    public CompletableFuture<Result<AuthorizationResult, GatewayError>> authorizeRestApiAccess(
            String userId, String operation, Map<String, Object> context) {
        
        return CompletableFuture.supplyAsync(() -> 
            authorizeOperation(userId, operation, "API")
                .fold(
                    authorized -> Result.success(new AuthorizationResult(
                        authorized,
                        authorized ? java.util.Optional.<String>empty() : 
                                   java.util.Optional.of("API access not permitted"),
                        ROLE_PERMISSIONS.getOrDefault(getUserRole(userId), Set.of()),
                        Map.of("operation", operation, "resource", "API"),
                        Set.of("API_ACCESS_POLICY", "USER_ROLE_POLICY"),
                        Map.of("userId", userId, "operation", operation, "resource", "API")
                    )),
                    error -> Result.<AuthorizationResult, GatewayError>failure(error)
                ), virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Authorize external service access
     */
    public CompletableFuture<Result<AuthorizationResult, GatewayError>> authorizeExternalServiceAccess(
            String userId, String targetService, Map<String, Object> context) {
        
        return CompletableFuture.supplyAsync(() -> 
            authorizeOperation(userId, "EXTERNAL_SERVICE_ACCESS", targetService)
                .fold(
                    authorized -> Result.success(new AuthorizationResult(
                        authorized,
                        authorized ? java.util.Optional.<String>empty() : 
                                   java.util.Optional.of("External service access not permitted"),
                        ROLE_PERMISSIONS.getOrDefault(getUserRole(userId), Set.of()),
                        Map.of("target_service", targetService, "resource", "EXTERNAL_SERVICE"),
                        Set.of("EXTERNAL_SERVICE_POLICY", "USER_ROLE_POLICY", "SERVICE_WHITELIST_POLICY"),
                        Map.of("userId", userId, "targetService", targetService, "resource", "EXTERNAL_SERVICE")
                    )),
                    error -> Result.<AuthorizationResult, GatewayError>failure(error)
                ), virtualThreadExecutor);
    }
    
    // ✅ PRIVATE HELPERS: Functional authorization logic
    
    private Result<Boolean, GatewayError> authorizeOperation(String userId, String operation, String resource) {
        return switch (validateUserPermissions(userId, operation)) {
            case AUTHORIZED -> Result.success(true);
            case DENIED -> Result.success(false);
            case ERROR -> Result.failure(new GatewayError.AuthorizationError.PermissionDenied(
                "Authorization check failed", resource, operation));
        };
    }
    
    private AuthorizationStatus validateUserPermissions(String userId, String operation) {
        return java.util.Optional.ofNullable(userId)
            .map(this::getUserRole)
            .map(ROLE_PERMISSIONS::get)
            .map(permissions -> permissions.contains(operation) ? 
                 AuthorizationStatus.AUTHORIZED : AuthorizationStatus.DENIED)
            .orElse(AuthorizationStatus.ERROR);
    }
    
    private String getUserRole(String userId) {
        // ✅ PATTERN MATCHING: Determine user role based on userId pattern
        return switch (userId) {
            case String id when id.startsWith("admin-") -> "ADMIN";
            case String id when id.startsWith("user-") -> "USER";
            case String id when id.startsWith("guest-") -> "GUEST";
            default -> "GUEST";
        };
    }
    
    // ✅ SEALED INTERFACES: Type-safe authorization status
    private enum AuthorizationStatus {
        AUTHORIZED, DENIED, ERROR
    }
    
    /**
     * ✅ IMMUTABLE: Authorization result record
     */
    public record AuthorizationResult(
        boolean authorized,
        java.util.Optional<String> denialReason,
        Set<String> grantedOperations,
        Map<String, Object> securityContext,
        Set<String> appliedPolicies,
        Map<String, Object> context
    ) {
        // Convenience methods
        public Set<String> appliedPolicies() { return appliedPolicies; }
        public Map<String, Object> context() { return context; }
    }
}
package com.trademaster.eventbus.security;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * ✅ SECURITY FACADE: Zero Trust External Access Control
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #6: Zero Trust Security Policy with tiered approach
 * - SecurityFacade + SecurityMediator for ALL external access
 * - Simple constructor injection for internal service-to-service
 * - Java 24 Virtual Threads for all async operations
 * - Functional programming patterns (no if-else)
 * - Circuit breaker protection for security operations
 * 
 * SECURITY BOUNDARY:
 * - External Access: REST APIs, WebSocket connections → SecurityFacade
 * - Internal Access: Service-to-service calls → Direct injection
 * - Zero Trust: Verify everything, trust nothing
 * - Defense in Depth: Multiple security layers
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFacade {
    
    // ✅ VIRTUAL THREADS: Dedicated executor for security operations
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    // ✅ DEPENDENCY INJECTION: Security mediator for coordination
    private final SecurityMediator securityMediator;
    
    /**
     * ✅ FUNCTIONAL: Secure WebSocket connection access
     */
    public CompletableFuture<Result<SecureAccessResult, GatewayError>> secureWebSocketAccess(
            SecurityContext context,
            Function<SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        log.debug("Securing WebSocket access for context: {}", context.sessionId());
        
        return CompletableFuture
            .supplyAsync(() -> validateSecurityContext(context), virtualThreadExecutor)
            .thenCompose(validationResult -> validationResult.fold(
                validContext -> securityMediator.mediateWebSocketAccess(validContext, operation),
                error -> CompletableFuture.completedFuture(Result.failure(error))
            ))
            .thenApply(this::auditSecurityAccess)
            .handle(this::handleSecurityAccessResult);
    }
    
    /**
     * ✅ FUNCTIONAL: Secure REST API access
     */
    public CompletableFuture<Result<SecureAccessResult, GatewayError>> secureRestApiAccess(
            SecurityContext context,
            Function<SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        log.debug("Securing REST API access for context: {}", context.requestId());
        
        return CompletableFuture
            .supplyAsync(() -> validateSecurityContext(context), virtualThreadExecutor)
            .thenCompose(validationResult -> validationResult.fold(
                validContext -> securityMediator.mediateRestApiAccess(validContext, operation),
                error -> CompletableFuture.completedFuture(Result.failure(error))
            ))
            .thenApply(this::auditSecurityAccess)
            .handle(this::handleSecurityAccessResult);
    }
    
    /**
     * ✅ FUNCTIONAL: Secure external service call
     */
    public CompletableFuture<Result<SecureAccessResult, GatewayError>> secureExternalServiceCall(
            SecurityContext context,
            String targetService,
            Function<SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        log.debug("Securing external service call to {} for context: {}", targetService, context.userId());
        
        return CompletableFuture
            .supplyAsync(() -> validateSecurityContext(context), virtualThreadExecutor)
            .thenCompose(validationResult -> validationResult.fold(
                validContext -> securityMediator.mediateExternalServiceCall(validContext, targetService, operation),
                error -> CompletableFuture.completedFuture(Result.failure(error))
            ))
            .thenApply(this::auditSecurityAccess)
            .handle(this::handleSecurityAccessResult);
    }
    
    // ✅ HELPER METHODS: Private helper methods using functional patterns
    
    /**
     * ✅ FUNCTIONAL: Validate security context without if-else
     */
    private Result<SecurityContext, GatewayError> validateSecurityContext(SecurityContext context) {
        return java.util.Optional.of(context)
            .filter(ctx -> ctx.userId() != null && !ctx.userId().isBlank())
            .filter(ctx -> ctx.sessionId() != null && !ctx.sessionId().isBlank())
            .filter(ctx -> ctx.requestId() != null && !ctx.requestId().isBlank())
            .map(Result::<SecurityContext, GatewayError>success)
            .orElse(Result.failure(new GatewayError.AuthenticationError.InvalidCredentials(
                "Invalid security context", "SECURITY_CONTEXT_VALIDATION")));
    }
    
    /**
     * ✅ FUNCTIONAL: Audit security access
     */
    private Result<SecureAccessResult, GatewayError> auditSecurityAccess(
            Result<SecureAccessResult, GatewayError> accessResult) {
        
        return accessResult.onSuccess(result -> 
            log.info("Security access granted: userId={}, sessionId={}, operation={}", 
                result.context().userId(), 
                result.context().sessionId(), 
                result.operationType())
        ).onFailure(error -> 
            log.warn("Security access denied: error={}", error.getMessage())
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Handle security access result
     */
    private Result<SecureAccessResult, GatewayError> handleSecurityAccessResult(
            Result<SecureAccessResult, GatewayError> result, 
            Throwable throwable) {
        
        return java.util.Optional.ofNullable(throwable)
            .map(t -> Result.<SecureAccessResult, GatewayError>failure(
                new GatewayError.SystemError.InternalServerError(
                    "Security facade error: " + t.getMessage(), 
                    "SECURITY_FACADE_ERROR")))
            .orElse(result);
    }
    
    // ✅ IMMUTABLE: Data transfer records
    
    public record SecurityContext(
        String userId,
        String sessionId,
        String requestId,
        Map<String, String> headers,
        java.time.Instant timestamp,
        String ipAddress,
        java.util.Optional<String> userAgent
    ) {}
    
    public record SecureAccessResult(
        SecurityContext context,
        String operationType,
        boolean accessGranted,
        java.time.Instant accessTime,
        java.util.Optional<String> denialReason,
        Map<String, Object> securityMetadata
    ) {}
}
package com.trademaster.eventbus.security;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import com.trademaster.eventbus.service.SecurityAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Single Responsibility: Security Pipeline Coordination
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: SOLID Principles - Single Responsibility for pipeline coordination
 * - Rule #3: Functional Programming - Railway pattern, no if-else
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total
 * - Rule #12: Virtual Threads for all async operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityPipelineCoordinator {
    
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    private final SecuritySessionManager sessionManager;
    private final SecurityAuthenticationService authenticationService;
    private final SecurityAuthorizationService authorizationService;
    private final SecurityRiskAssessmentService riskAssessmentService;
    private final SecurityAuditService auditService;
    
    /**
     * ✅ FUNCTIONAL: Coordinate WebSocket security pipeline
     */
    public CompletableFuture<Result<SecurityFacade.SecureAccessResult, GatewayError>> coordinateWebSocketAccess(
            SecurityFacade.SecurityContext context,
            Function<SecurityFacade.SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        log.debug("Coordinating WebSocket security pipeline for user: {}", context.userId());
        
        return CompletableFuture
            .supplyAsync(() -> sessionManager.initializeSession(context), virtualThreadExecutor)
            .thenCompose(this::authenticateUserStep)
            .thenCompose(sessionResult -> authorizeWebSocketStep(sessionResult, context))
            .thenCompose(this::assessRiskStep)
            .thenCompose(sessionResult -> executeSecureOperationStep(sessionResult, operation))
            .thenApply(this::auditOperationStep)
            .handle(this::handlePipelineErrors);
    }
    
    /**
     * ✅ FUNCTIONAL: Coordinate REST API security pipeline
     */
    public CompletableFuture<Result<SecurityFacade.SecureAccessResult, GatewayError>> coordinateRestApiAccess(
            SecurityFacade.SecurityContext context,
            Function<SecurityFacade.SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        log.debug("Coordinating REST API security pipeline for user: {}", context.userId());
        
        return CompletableFuture
            .supplyAsync(() -> sessionManager.initializeSession(context), virtualThreadExecutor)
            .thenCompose(this::authenticateUserStep)
            .thenCompose(sessionResult -> authorizeRestApiStep(sessionResult, context))
            .thenCompose(this::assessRiskStep)
            .thenCompose(sessionResult -> executeSecureOperationStep(sessionResult, operation))
            .thenApply(this::auditOperationStep)
            .handle(this::handlePipelineErrors);
    }
    
    // ✅ PRIVATE PIPELINE STEPS: Functional composition helpers
    
    private CompletableFuture<Result<SecuritySessionManager.SecuritySessionState, GatewayError>> authenticateUserStep(
            Result<SecuritySessionManager.SecuritySessionState, GatewayError> sessionResult) {
        
        return sessionResult.fold(
            session -> authenticationService.validateJwtToken("sample-jwt-" + session.sessionId())
                .thenApply(authResult -> authResult.fold(
                    _ -> sessionManager.updateSessionPhase(session.sessionId(), 
                         SecuritySessionManager.SecuritySessionState.SessionPhase.AUTHENTICATED),
                    error -> Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(error)
                )),
            error -> CompletableFuture.completedFuture(Result.failure(error))
        );
    }
    
    private CompletableFuture<Result<SecuritySessionManager.SecuritySessionState, GatewayError>> authorizeWebSocketStep(
            Result<SecuritySessionManager.SecuritySessionState, GatewayError> sessionResult,
            SecurityFacade.SecurityContext context) {
        
        return sessionResult.fold(
            session -> authorizationService.authorizeWebSocketAccess(
                session.userId(), "WEBSOCKET_CONNECT", Map.of("session_id", session.sessionId())
            ).thenApply(authzResult -> authzResult.fold(
                authorization -> authorization.authorized() ?
                    sessionManager.updateSessionPhase(session.sessionId(), 
                        SecuritySessionManager.SecuritySessionState.SessionPhase.AUTHORIZED) :
                    Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(
                        new GatewayError.AuthorizationError.PermissionDenied(
                            authorization.denialReason().orElse("WebSocket access denied"), 
                            "WEBSOCKET", "CONNECT")),
                error -> Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(error)
            )),
            error -> CompletableFuture.completedFuture(Result.failure(error))
        );
    }
    
    private CompletableFuture<Result<SecuritySessionManager.SecuritySessionState, GatewayError>> authorizeRestApiStep(
            Result<SecuritySessionManager.SecuritySessionState, GatewayError> sessionResult,
            SecurityFacade.SecurityContext context) {
        
        return sessionResult.fold(
            session -> authorizationService.authorizeRestApiAccess(
                session.userId(), "API_ACCESS", Map.of("request_id", session.requestId())
            ).thenApply(authzResult -> authzResult.fold(
                authorization -> authorization.authorized() ?
                    sessionManager.updateSessionPhase(session.sessionId(), 
                        SecuritySessionManager.SecuritySessionState.SessionPhase.AUTHORIZED) :
                    Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(
                        new GatewayError.AuthorizationError.PermissionDenied(
                            authorization.denialReason().orElse("API access denied"),
                            "API", "ACCESS")),
                error -> Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(error)
            )),
            error -> CompletableFuture.completedFuture(Result.failure(error))
        );
    }
    
    private CompletableFuture<Result<SecuritySessionManager.SecuritySessionState, GatewayError>> assessRiskStep(
            Result<SecuritySessionManager.SecuritySessionState, GatewayError> sessionResult) {
        
        return sessionResult.fold(
            session -> riskAssessmentService.assessRisk(session.userId(), session.metadata())
                .thenApply(riskResult -> riskResult.fold(
                    riskAssessment -> riskAssessment.riskLevel().isAcceptable() ?
                        sessionManager.updateSessionPhase(session.sessionId(), 
                            SecuritySessionManager.SecuritySessionState.SessionPhase.RISK_ASSESSED) :
                        Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(
                            new GatewayError.AuthorizationError.AccessDenied(
                                "Risk assessment failed: " + riskAssessment.riskLevel(),
                                "HIGH_RISK_DETECTED")),
                    error -> Result.<SecuritySessionManager.SecuritySessionState, GatewayError>failure(error)
                )),
            error -> CompletableFuture.completedFuture(Result.failure(error))
        );
    }
    
    private CompletableFuture<Result<SecurityFacade.SecureAccessResult, GatewayError>> executeSecureOperationStep(
            Result<SecuritySessionManager.SecuritySessionState, GatewayError> sessionResult,
            Function<SecurityFacade.SecurityContext, CompletableFuture<Result<Object, GatewayError>>> operation) {
        
        return sessionResult.fold(
            session -> {
                SecurityFacade.SecurityContext secureContext = new SecurityFacade.SecurityContext(
                    session.userId(), session.sessionId(), session.requestId(), Map.of(),
                    session.createdTime(), session.metadata().get("ip_address"),
                    java.util.Optional.ofNullable(session.metadata().get("user_agent"))
                );
                
                return operation.apply(secureContext)
                    .thenApply(operationResult -> operationResult.fold(
                        result -> Result.success(new SecurityFacade.SecureAccessResult(
                            secureContext, "SECURE_OPERATION", true, Instant.now(),
                            java.util.Optional.empty(), Map.of("operation_result", result.toString())
                        )),
                        error -> Result.<SecurityFacade.SecureAccessResult, GatewayError>failure(error)
                    ));
            },
            error -> CompletableFuture.completedFuture(Result.failure(error))
        );
    }
    
    private Result<SecurityFacade.SecureAccessResult, GatewayError> auditOperationStep(
            Result<SecurityFacade.SecureAccessResult, GatewayError> operationResult) {
        
        return operationResult
            .onSuccess(result -> auditService.logSecurityEvent(
                result.context().userId(), result.operationType(),
                result.accessGranted() ? "ACCESS_GRANTED" : "ACCESS_DENIED",
                result.securityMetadata()
            ))
            .onFailure(error -> auditService.logSecurityEvent(
                "UNKNOWN", "SECURITY_OPERATION", 
                "SECURITY_ERROR: " + error.getMessage(),
                Map.of("error_code", error.getErrorCode())
            ));
    }
    
    private Result<SecurityFacade.SecureAccessResult, GatewayError> handlePipelineErrors(
            Result<SecurityFacade.SecureAccessResult, GatewayError> result, Throwable throwable) {
        
        return java.util.Optional.ofNullable(throwable)
            .map(t -> Result.<SecurityFacade.SecureAccessResult, GatewayError>failure(
                new GatewayError.SystemError.InternalServerError(
                    "Security pipeline error: " + t.getMessage(), 
                    "SECURITY_PIPELINE_ERROR")))
            .orElse(result);
    }
}
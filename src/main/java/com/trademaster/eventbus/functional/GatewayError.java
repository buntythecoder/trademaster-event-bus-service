package com.trademaster.eventbus.functional;

/**
 * ✅ IMMUTABLE: Gateway Error Hierarchy
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface hierarchy for type safety
 * - Immutable records for all error data
 * - Pattern matching support for error handling
 * - No if-else statements in error processing
 * - Comprehensive error categorization
 * 
 * ERROR CATEGORIES:
 * - Authentication: JWT validation, session issues
 * - Authorization: Permission denied, rate limiting  
 * - Connection: WebSocket connection failures
 * - Message: Parsing, validation, formatting errors
 * - System: Internal service failures, circuit breakers
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per interface
 */
public sealed interface GatewayError permits
    GatewayError.AuthenticationError,
    GatewayError.AuthorizationError, 
    GatewayError.ConnectionError,
    GatewayError.MessageError,
    GatewayError.SystemError,
    GatewayError.ProcessingError,
    GatewayError.ServiceError,
    GatewayError.ValidationError,
    GatewayError.SecurityError {
    
    // ✅ FUNCTIONAL: Error message accessor
    String getMessage();
    
    // ✅ FUNCTIONAL: Error code accessor  
    String getErrorCode();
    
    // ✅ FUNCTIONAL: Error severity level
    ErrorSeverity getSeverity();
    
    // ✅ IMMUTABLE: Error severity levels
    enum ErrorSeverity {
        LOW,     // Informational, user can continue
        MEDIUM,  // Warning, degraded functionality
        HIGH,    // Error, operation failed
        CRITICAL // System failure, immediate attention required
    }
    
    // ✅ AUTHENTICATION ERRORS: JWT and session related
    sealed interface AuthenticationError extends GatewayError permits
        InvalidToken, ExpiredToken, MissingToken, InvalidCredentials, InvalidSession, SessionExpired, TokenGenerationFailed {
    }
    
    record InvalidToken(String message, String tokenType) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_001_INVALID_TOKEN"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ExpiredToken(String message, java.time.Instant expiredAt) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_002_EXPIRED_TOKEN"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record MissingToken(String message) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_003_MISSING_TOKEN"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record InvalidCredentials(String message, String userId) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_004_INVALID_CREDENTIALS"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record InvalidSession(String message, String sessionId) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_005_INVALID_SESSION"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record SessionExpired(String message, java.time.Instant expiredAt) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_006_SESSION_EXPIRED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record TokenGenerationFailed(String message, String reason) implements AuthenticationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTH_007_TOKEN_GENERATION_FAILED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    // ✅ AUTHORIZATION ERRORS: Permission and rate limiting
    sealed interface AuthorizationError extends GatewayError permits
        PermissionDenied, AuthRateLimitExceeded, SubscriptionRequired, AccessDenied, InsufficientPermissions {
    }
    
    record PermissionDenied(String message, String resource, String action) implements AuthorizationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTHZ_001_PERMISSION_DENIED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record AuthRateLimitExceeded(String message, int currentRate, int maxRate, java.time.Duration resetTime) implements AuthorizationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTHZ_002_RATE_LIMIT_EXCEEDED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record SubscriptionRequired(String message, String requiredPlan) implements AuthorizationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTHZ_003_SUBSCRIPTION_REQUIRED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record AccessDenied(String message, String reason) implements AuthorizationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTHZ_004_ACCESS_DENIED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record InsufficientPermissions(String message, String resource) implements AuthorizationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "AUTHZ_005_INSUFFICIENT_PERMISSIONS"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    // ✅ CONNECTION ERRORS: WebSocket connection issues
    sealed interface ConnectionError extends GatewayError permits
        ConnectionFailed, ConnectionTimeout, NoActiveConnections, ConnectionLimitExceeded, TooManyConnections, ConnRateLimitExceeded {
    }
    
    record ConnectionFailed(String message) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_001_CONNECTION_FAILED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ConnectionTimeout(String message, java.time.Duration timeout) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_002_CONNECTION_TIMEOUT"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record NoActiveConnections(String message) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_003_NO_ACTIVE_CONNECTIONS"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.LOW; }
    }
    
    record ConnectionLimitExceeded(String message, int currentConnections, int maxConnections) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_004_CONNECTION_LIMIT_EXCEEDED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record TooManyConnections(String message, int maxConnections) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_005_TOO_MANY_CONNECTIONS"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ConnRateLimitExceeded(String message, String resource) implements ConnectionError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "CONN_006_RATE_LIMIT_EXCEEDED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    // ✅ MESSAGE ERRORS: Parsing and validation issues
    sealed interface MessageError extends GatewayError permits
        InvalidMessageFormat, MessageTooLarge, UnsupportedMessageType, MessageValidationFailed, InvalidFormat, UnsupportedType, EmptyMessage, MissingField {
    }
    
    record InvalidMessageFormat(String message, String receivedFormat, String expectedFormat) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_001_INVALID_FORMAT"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record MessageTooLarge(String message, int messageSize, int maxSize) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_002_MESSAGE_TOO_LARGE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record UnsupportedMessageType(String message, String messageType) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_003_UNSUPPORTED_TYPE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record MessageValidationFailed(String message, java.util.List<String> validationErrors) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_004_VALIDATION_FAILED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record InvalidFormat(String message, String format) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_005_INVALID_FORMAT"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record UnsupportedType(String message, String type) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_006_UNSUPPORTED_TYPE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record EmptyMessage(String message, String context) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_007_EMPTY_MESSAGE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record MissingField(String message, String field) implements MessageError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "MSG_008_MISSING_FIELD"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    // ✅ VALIDATION ERRORS: Input validation issues
    sealed interface ValidationError extends GatewayError permits
        InvalidInput, InvalidValidationFormat, InvalidType, ValidationFailed {
    }
    
    record InvalidInput(String message, String field, String value) implements ValidationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "VAL_001_INVALID_INPUT"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record InvalidValidationFormat(String message, String field) implements ValidationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "VAL_004_INVALID_FORMAT"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record ValidationFailed(String message, java.util.List<String> errors) implements ValidationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "VAL_002_VALIDATION_FAILED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record InvalidType(String message, String expectedType, String actualType) implements ValidationError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "VAL_003_INVALID_TYPE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    // ✅ SYSTEM ERRORS: Internal service failures
    sealed interface SystemError extends GatewayError permits
        ServiceUnavailable, CircuitBreakerOpen, InternalServerError, ConfigurationError,
        DatabaseError, MessageQueueError, ExternalServiceError, WebSocketError, SystemProcessingError, TimeoutError, ResourceExhaustion {
    }
    
    // ✅ PROCESSING ERRORS: Message and workflow processing
    sealed interface ProcessingError extends GatewayError permits
        MessageProcessingFailed {
    }
    
    record MessageProcessingFailed(String message, String errorCode) implements ProcessingError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return errorCode; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    // ✅ SERVICE ERRORS: External service integration
    sealed interface ServiceError extends GatewayError permits
        SubscriptionServiceError {
    }
    
    record SubscriptionServiceError(String message, String serviceId) implements ServiceError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SVC_001_SUBSCRIPTION_SERVICE_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ServiceUnavailable(String message, String serviceName) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_001_SERVICE_UNAVAILABLE"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.CRITICAL; }
    }
    
    record CircuitBreakerOpen(String message, String serviceName, java.time.Duration resetTime) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_002_CIRCUIT_BREAKER_OPEN"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record InternalServerError(String message, String errorId) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_003_INTERNAL_SERVER_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.CRITICAL; }
    }
    
    record ConfigurationError(String message, String configurationKey) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_004_CONFIGURATION_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.CRITICAL; }
    }
    
    record DatabaseError(String message, String operation) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_005_DATABASE_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record MessageQueueError(String message, String operation) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_006_MESSAGE_QUEUE_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ExternalServiceError(String message, String serviceName) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_007_EXTERNAL_SERVICE_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record WebSocketError(String message, String operation) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_008_WEBSOCKET_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.MEDIUM; }
    }
    
    record SystemProcessingError(String message, String processingType) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_009_PROCESSING_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record TimeoutError(String message, java.time.Duration timeout) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_010_TIMEOUT_ERROR"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record ResourceExhaustion(String message, String resourceType) implements SystemError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SYS_011_RESOURCE_EXHAUSTION"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.CRITICAL; }
    }
    
    // ✅ SECURITY ERRORS: Security related issues
    sealed interface SecurityError extends GatewayError permits
        Unauthorized, Forbidden {
    }
    
    record Unauthorized(String message, String resource) implements SecurityError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SEC_001_UNAUTHORIZED"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
    
    record Forbidden(String message, String resource) implements SecurityError {
        @Override
        public String getMessage() { return message; }
        
        @Override
        public String getErrorCode() { return "SEC_002_FORBIDDEN"; }
        
        @Override
        public ErrorSeverity getSeverity() { return ErrorSeverity.HIGH; }
    }
}
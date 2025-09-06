package com.trademaster.eventbus.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Functional error type for gateway operations.
 * Immutable record with error classification and context.
 */
public record GatewayError(
    ErrorType type,
    String message,
    String correlationId,
    Instant timestamp,
    String source
) {
    
    public GatewayError {
        Objects.requireNonNull(type, "Error type cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(correlationId, "Correlation ID cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(source, "Source cannot be null");
    }
    
    /**
     * Error classification for different types of gateway failures
     */
    public enum ErrorType {
        // Authentication & Authorization
        AUTHENTICATION_FAILED("Authentication failure"),
        AUTHORIZATION_DENIED("Authorization denied"),
        TOKEN_EXPIRED("Token expired"),
        
        // Validation Errors
        VALIDATION_ERROR("Input validation failed"),
        INVALID_FORMAT("Invalid data format"),
        MISSING_REQUIRED_FIELD("Required field missing"),
        
        // Connection & Network
        CONNECTION_TIMEOUT("Connection timeout"),
        NETWORK_ERROR("Network connectivity issue"),
        SERVICE_UNAVAILABLE("Service temporarily unavailable"),
        
        // Rate Limiting & Throttling
        RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
        QUOTA_EXCEEDED("Quota limit exceeded"),
        THROTTLING_ACTIVE("Request throttled"),
        
        // Circuit Breaker
        CIRCUIT_BREAKER_OPEN("Circuit breaker open"),
        CIRCUIT_BREAKER_TIMEOUT("Circuit breaker timeout"),
        
        // Resource Issues
        RESOURCE_NOT_FOUND("Resource not found"),
        RESOURCE_LOCKED("Resource temporarily locked"),
        RESOURCE_EXHAUSTED("Resource capacity exhausted"),
        
        // Processing Errors
        PROCESSING_ERROR("Processing failed"),
        TRANSFORMATION_ERROR("Data transformation failed"),
        SERIALIZATION_ERROR("Serialization failed"),
        
        // System Errors
        INTERNAL_ERROR("Internal system error"),
        CONFIGURATION_ERROR("Configuration error"),
        DEPENDENCY_FAILURE("External dependency failed"),
        
        // Business Logic
        BUSINESS_RULE_VIOLATION("Business rule violated"),
        WORKFLOW_ERROR("Workflow execution failed"),
        STATE_CONFLICT("Resource state conflict"),
        
        // Agent/System Specific
        AGENT_INITIALIZATION_FAILED("Agent initialization failed"),
        HEALTH_CHECK_FAILED("Health check failed"),
        MCP_MESSAGE_FAILED("MCP message processing failed"),
        COMMAND_EXECUTION_FAILED("Command execution failed");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Check if error is retryable
         */
        public boolean isRetryable() {
            return switch (this) {
                case CONNECTION_TIMEOUT, NETWORK_ERROR, SERVICE_UNAVAILABLE,
                     THROTTLING_ACTIVE, CIRCUIT_BREAKER_TIMEOUT, RESOURCE_LOCKED,
                     DEPENDENCY_FAILURE -> true;
                default -> false;
            };
        }
        
        /**
         * Check if error is a client error (4xx equivalent)
         */
        public boolean isClientError() {
            return switch (this) {
                case AUTHENTICATION_FAILED, AUTHORIZATION_DENIED, TOKEN_EXPIRED,
                     VALIDATION_ERROR, INVALID_FORMAT, MISSING_REQUIRED_FIELD,
                     RATE_LIMIT_EXCEEDED, QUOTA_EXCEEDED, RESOURCE_NOT_FOUND,
                     BUSINESS_RULE_VIOLATION -> true;
                default -> false;
            };
        }
        
        /**
         * Check if error is a server error (5xx equivalent)
         */
        public boolean isServerError() {
            return switch (this) {
                case SERVICE_UNAVAILABLE, CIRCUIT_BREAKER_OPEN, CIRCUIT_BREAKER_TIMEOUT,
                     RESOURCE_EXHAUSTED, PROCESSING_ERROR, TRANSFORMATION_ERROR,
                     SERIALIZATION_ERROR, INTERNAL_ERROR, CONFIGURATION_ERROR,
                     DEPENDENCY_FAILURE, WORKFLOW_ERROR, STATE_CONFLICT -> true;
                default -> false;
            };
        }
    }
    
    /**
     * Factory methods for common error types
     */
    public static GatewayError authenticationFailed(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.AUTHENTICATION_FAILED,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError authorizationDenied(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.AUTHORIZATION_DENIED,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError validationError(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.VALIDATION_ERROR,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError rateLimitExceeded(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.RATE_LIMIT_EXCEEDED,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError circuitBreakerOpen(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.CIRCUIT_BREAKER_OPEN,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError connectionTimeout(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.CONNECTION_TIMEOUT,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError serviceUnavailable(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.SERVICE_UNAVAILABLE,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError processingError(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.PROCESSING_ERROR,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError internalError(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.INTERNAL_ERROR,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    public static GatewayError businessRuleViolation(String message, String correlationId, String source) {
        return new GatewayError(
            ErrorType.BUSINESS_RULE_VIOLATION,
            message,
            correlationId,
            Instant.now(),
            source
        );
    }
    
    /**
     * Create error with current timestamp
     */
    public static GatewayError of(ErrorType type, String message, String correlationId, String source) {
        return new GatewayError(type, message, correlationId, Instant.now(), source);
    }
    
    /**
     * Check if this error is retryable
     */
    public boolean isRetryable() {
        return type.isRetryable();
    }
    
    /**
     * Check if this is a client error
     */
    public boolean isClientError() {
        return type.isClientError();
    }
    
    /**
     * Check if this is a server error
     */
    public boolean isServerError() {
        return type.isServerError();
    }
    
    /**
     * Get formatted error message for logging
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s (correlationId=%s, source=%s, timestamp=%s)",
            type.name(), message, correlationId, source, timestamp);
    }
}
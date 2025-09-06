package com.trademaster.eventbus.service;

import com.trademaster.eventbus.domain.Result;
import com.trademaster.eventbus.domain.GatewayError;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Circuit Breaker Service for Event Bus Operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {
    
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    private final CircuitBreaker databaseCircuitBreaker;
    private final CircuitBreaker messageQueueCircuitBreaker;
    private final CircuitBreaker externalServiceCircuitBreaker;
    private final CircuitBreaker websocketCircuitBreaker;
    private final CircuitBreaker eventProcessingCircuitBreaker;
    
    /**
     * Execute database operation with circuit breaker
     */
    public <T> CompletableFuture<Result<T, GatewayError>> executeDatabaseOperation(
            Supplier<T> operation, 
            Supplier<T> fallback,
            String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(databaseCircuitBreaker, operation);
                T result = decoratedSupplier.get();
                return Result.<T, GatewayError>success(result);
            } catch (Exception e) {
                log.error("Database operation '{}' failed: {}", operationName, e.getMessage());
                if (fallback != null) {
                    try {
                        T fallbackResult = fallback.get();
                        return Result.<T, GatewayError>success(fallbackResult);
                    } catch (Exception fallbackError) {
                        log.error("Fallback for '{}' also failed: {}", operationName, fallbackError.getMessage());
                    }
                }
                return Result.<T, GatewayError>failure(
                    GatewayError.processingError(
                        "Database operation failed: " + e.getMessage(),
                        "db-circuit-" + System.currentTimeMillis(),
                        operationName));
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * Execute message queue operation with circuit breaker
     */
    public <T> CompletableFuture<Result<T, GatewayError>> executeMessageQueueOperation(
            Supplier<T> operation,
            String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(messageQueueCircuitBreaker, operation);
                T result = decoratedSupplier.get();
                return Result.<T, GatewayError>success(result);
            } catch (Exception e) {
                log.error("Message queue operation '{}' failed: {}", operationName, e.getMessage());
                return Result.<T, GatewayError>failure(
                    GatewayError.processingError(
                        "Message queue operation failed: " + e.getMessage(),
                        "mq-circuit-" + System.currentTimeMillis(),
                        operationName));
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * Execute external service call with circuit breaker
     */
    public <T> CompletableFuture<Result<T, GatewayError>> executeExternalServiceCall(
            Supplier<T> serviceCall,
            Supplier<T> fallback,
            String serviceName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(externalServiceCircuitBreaker, serviceCall);
                T result = decoratedSupplier.get();
                return Result.<T, GatewayError>success(result);
            } catch (Exception e) {
                log.error("External service '{}' call failed: {}", serviceName, e.getMessage());
                if (fallback != null) {
                    try {
                        T fallbackResult = fallback.get();
                        log.warn("Using fallback for external service '{}'", serviceName);
                        return Result.<T, GatewayError>success(fallbackResult);
                    } catch (Exception fallbackError) {
                        log.error("Fallback for '{}' also failed: {}", serviceName, fallbackError.getMessage());
                    }
                }
                return Result.<T, GatewayError>failure(
                    GatewayError.serviceUnavailable(
                        "External service call failed: " + e.getMessage(),
                        "ext-circuit-" + System.currentTimeMillis(),
                        serviceName));
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * Execute WebSocket operation with circuit breaker
     */
    public <T> CompletableFuture<Result<T, GatewayError>> executeWebSocketOperation(
            Supplier<T> operation,
            String operationName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(websocketCircuitBreaker, operation);
                T result = decoratedSupplier.get();
                return Result.<T, GatewayError>success(result);
            } catch (Exception e) {
                log.error("WebSocket operation '{}' failed: {}", operationName, e.getMessage());
                return Result.<T, GatewayError>failure(
                    GatewayError.connectionTimeout(
                        "WebSocket operation failed: " + e.getMessage(),
                        "ws-circuit-" + System.currentTimeMillis(),
                        operationName));
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * Execute event processing with circuit breaker
     */
    public <T> CompletableFuture<Result<T, GatewayError>> executeEventProcessing(
            Supplier<T> processing,
            Supplier<T> degradedProcessing,
            String processingType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(eventProcessingCircuitBreaker, processing);
                T result = decoratedSupplier.get();
                return Result.<T, GatewayError>success(result);
            } catch (Exception e) {
                log.error("Event processing '{}' failed: {}", processingType, e.getMessage());
                if (degradedProcessing != null) {
                    try {
                        T degradedResult = degradedProcessing.get();
                        log.warn("Using degraded processing for '{}'", processingType);
                        return Result.<T, GatewayError>success(degradedResult);
                    } catch (Exception degradedError) {
                        log.error("Degraded processing for '{}' also failed: {}", processingType, degradedError.getMessage());
                    }
                }
                return Result.<T, GatewayError>failure(
                    GatewayError.processingError(
                        "Event processing failed: " + e.getMessage(),
                        "event-circuit-" + System.currentTimeMillis(),
                        processingType));
            }
        }, virtualThreadExecutor);
    }
    
    // MISSING METHODS REQUIRED BY CONTROLLERS
    
    /**
     * Get database circuit breaker status
     */
    public String getDatabaseCircuitBreakerStatus() {
        return databaseCircuitBreaker.getState().name();
    }
    
    /**
     * Get message queue circuit breaker status
     */
    public String getMessageQueueCircuitBreakerStatus() {
        return messageQueueCircuitBreaker.getState().name();
    }
    
    /**
     * Get external service circuit breaker status
     */
    public String getExternalServiceCircuitBreakerStatus() {
        return externalServiceCircuitBreaker.getState().name();
    }
    
    /**
     * Get WebSocket circuit breaker status
     */
    public String getWebSocketCircuitBreakerStatus() {
        return websocketCircuitBreaker.getState().name();
    }
    
    /**
     * Get event processing circuit breaker status
     */
    public String getEventProcessingCircuitBreakerStatus() {
        return eventProcessingCircuitBreaker.getState().name();
    }
    
    /**
     * Force circuit breaker open
     */
    public void forceCircuitBreakerOpen(String circuitBreakerName) {
        getCircuitBreakerByName(circuitBreakerName).ifPresent(cb -> {
            cb.transitionToOpenState();
            log.warn("Circuit breaker '{}' forced to OPEN state", circuitBreakerName);
        });
    }
    
    /**
     * Force circuit breaker closed
     */
    public void forceCircuitBreakerClosed(String circuitBreakerName) {
        getCircuitBreakerByName(circuitBreakerName).ifPresent(cb -> {
            cb.transitionToClosedState();
            log.info("Circuit breaker '{}' forced to CLOSED state", circuitBreakerName);
        });
    }
    
    /**
     * Force circuit breaker half-open
     */
    public void forceCircuitBreakerHalfOpen(String circuitBreakerName) {
        getCircuitBreakerByName(circuitBreakerName).ifPresent(cb -> {
            cb.transitionToHalfOpenState();
            log.info("Circuit breaker '{}' forced to HALF_OPEN state", circuitBreakerName);
        });
    }
    
    /**
     * Get circuit breaker by name
     */
    private java.util.Optional<CircuitBreaker> getCircuitBreakerByName(String name) {
        return switch (name.toLowerCase()) {
            case "database" -> java.util.Optional.of(databaseCircuitBreaker);
            case "messagequeue" -> java.util.Optional.of(messageQueueCircuitBreaker);
            case "externalservice" -> java.util.Optional.of(externalServiceCircuitBreaker);
            case "websocket" -> java.util.Optional.of(websocketCircuitBreaker);
            case "eventprocessing" -> java.util.Optional.of(eventProcessingCircuitBreaker);
            default -> {
                log.warn("Unknown circuit breaker name: {}", name);
                yield java.util.Optional.empty();
            }
        };
    }
    
    /**
     * Get circuit breaker health status
     */
    public CircuitBreakerHealth getCircuitBreakerHealth() {
        return new CircuitBreakerHealth(
            databaseCircuitBreaker.getState().name(),
            messageQueueCircuitBreaker.getState().name(),
            externalServiceCircuitBreaker.getState().name(),
            websocketCircuitBreaker.getState().name(),
            eventProcessingCircuitBreaker.getState().name(),
            Instant.now()
        );
    }
    
    /**
     * Circuit breaker health record
     */
    public record CircuitBreakerHealth(
        String databaseState,
        String messageQueueState,
        String externalServiceState,
        String websocketState,
        String eventProcessingState,
        Instant checkedAt
    ) {}
}
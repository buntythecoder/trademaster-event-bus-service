package com.trademaster.eventbus.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * ✅ CIRCUIT BREAKER CONFIGURATION: Production-Ready Resilience4j Setup
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #25: Circuit Breakers for ALL external calls and critical operations
 * - Rule #16: Dynamic Configuration with external properties
 * - Java 24 Virtual Threads compatibility
 * - Functional programming patterns
 * 
 * COVERAGE:
 * - Database operations
 * - Message queue operations  
 * - External service calls
 * - WebSocket operations
 * - Event processing pipeline
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerConfiguration {
    
    @Value("${trademaster.circuit-breaker.failure-rate-threshold:50}")
    private int failureRateThreshold;
    
    @Value("${trademaster.circuit-breaker.wait-duration-in-open-state:60s}")
    private Duration waitDurationInOpenState;
    
    @Value("${trademaster.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;
    
    @Value("${trademaster.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;
    
    /**
     * ✅ FUNCTIONAL: Database Circuit Breaker
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .waitDurationInOpenState(waitDurationInOpenState)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .recordExceptions(
                java.sql.SQLException.class,
                org.springframework.dao.DataAccessException.class,
                java.util.concurrent.TimeoutException.class
            )
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.of("database", config);
        
        // ✅ EVENT LISTENERS: Production monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Database Circuit Breaker state transition: {} -> {} at {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState(),
                    event.getCreationTime()))
            .onFailureRateExceeded(event -> 
                log.warn("Database Circuit Breaker failure rate exceeded: {}% at {}", 
                    String.format("%.1f", event.getFailureRate()), event.getCreationTime()))
            .onCallNotPermitted(event -> 
                log.warn("Database Circuit Breaker call not permitted at {}", event.getCreationTime()));
                
        return circuitBreaker;
    }
    
    /**
     * ✅ FUNCTIONAL: Message Queue Circuit Breaker  
     */
    @Bean
    public CircuitBreaker messageQueueCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60) // Higher threshold for message queue
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .recordExceptions(
                org.apache.kafka.common.errors.TimeoutException.class,
                org.springframework.kafka.KafkaException.class,
                java.util.concurrent.TimeoutException.class
            )
            .recordException(throwable -> !isRetriableException(throwable))
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.of("messageQueue", config);
        
        // ✅ EVENT LISTENERS: Message queue specific monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Message Queue Circuit Breaker state transition: {} -> {} at {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState(),
                    event.getCreationTime()))
            .onCallNotPermitted(event -> 
                log.warn("Message Queue Circuit Breaker blocked call - system overloaded at {}", 
                    event.getCreationTime()));
                    
        return circuitBreaker;
    }
    
    /**
     * ✅ FUNCTIONAL: External Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker externalServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40) // Lower threshold for external services
            .waitDurationInOpenState(Duration.ofMinutes(2))
            .slidingWindowSize(15)
            .minimumNumberOfCalls(8)
            .recordExceptions(
                java.io.IOException.class,
                java.net.SocketTimeoutException.class,
                java.util.concurrent.TimeoutException.class,
                org.springframework.web.client.ResourceAccessException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class,
                org.springframework.web.client.HttpClientErrorException.class
            )
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.of("externalService", config);
        
        // ✅ EVENT LISTENERS: External service monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("External Service Circuit Breaker state transition: {} -> {} - External dependency failure", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event -> 
                log.error("External Service Circuit Breaker failure rate {}% exceeded - Circuit OPENED", 
                    String.format("%.1f", event.getFailureRate())));
                    
        return circuitBreaker;
    }
    
    /**
     * ✅ FUNCTIONAL: WebSocket Circuit Breaker
     */
    @Bean
    public CircuitBreaker websocketCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(70) // Higher threshold for WebSocket (transient network issues)
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .slidingWindowSize(25)
            .minimumNumberOfCalls(15)
            .recordExceptions(
                java.io.IOException.class,
                java.util.concurrent.TimeoutException.class,
                RuntimeException.class
            )
            .recordException(throwable -> !(throwable instanceof InterruptedException))
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.of("websocket", config);
        
        // ✅ EVENT LISTENERS: WebSocket specific monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("WebSocket Circuit Breaker state change: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event -> 
                log.warn("WebSocket Circuit Breaker rejecting connection - system protection active"));
                
        return circuitBreaker;
    }
    
    /**
     * ✅ FUNCTIONAL: Event Processing Circuit Breaker
     */
    @Bean
    public CircuitBreaker eventProcessingCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(55)
            .waitDurationInOpenState(Duration.ofSeconds(90))
            .slidingWindowSize(30)
            .minimumNumberOfCalls(12)
            .recordExceptions(
                RuntimeException.class,
                java.util.concurrent.TimeoutException.class,
                java.util.concurrent.RejectedExecutionException.class
            )
            .recordException(throwable -> !isBusinessLogicException(throwable))
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.of("eventProcessing", config);
        
        // ✅ EVENT LISTENERS: Event processing monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Event Processing Circuit Breaker state change: {} -> {} - Processing pipeline affected", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onSlowCallRateExceeded(event -> 
                log.warn("Event Processing Circuit Breaker slow call rate exceeded: {}%", 
                    String.format("%.1f", event.getSlowCallRate())));
                    
        return circuitBreaker;
    }
    
    /**
     * ✅ FUNCTIONAL: Circuit Breaker Registry for dynamic access
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            CircuitBreaker databaseCircuitBreaker,
            CircuitBreaker messageQueueCircuitBreaker,
            CircuitBreaker externalServiceCircuitBreaker,
            CircuitBreaker websocketCircuitBreaker,
            CircuitBreaker eventProcessingCircuitBreaker) {
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.addConfiguration("trademaster-standard", CircuitBreakerConfig.ofDefaults());
        
        return registry;
    }
    
    // ✅ PRIVATE HELPERS: Exception classification
    
    private boolean isRetriableException(Throwable throwable) {
        return throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof java.util.concurrent.TimeoutException ||
               throwable instanceof org.apache.kafka.common.errors.TimeoutException;
    }
    
    private boolean isBusinessLogicException(Throwable throwable) {
        return throwable instanceof IllegalArgumentException ||
               throwable instanceof IllegalStateException ||
               throwable.getMessage().contains("business rule");
    }
}
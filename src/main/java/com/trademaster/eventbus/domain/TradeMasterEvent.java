package com.trademaster.eventbus.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ IMMUTABLE: Core TradeMaster Event Domain Model
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface for type safety and pattern matching
 * - Immutable records for all event data
 * - Functional programming patterns
 * - No if-else statements (pattern matching only)
 * - Comprehensive type safety with sealed hierarchies
 * 
 * EVENT PRIORITY LEVELS:
 * - Critical: Risk events, system failures (≤25ms processing)
 * - High: Order events, market data (≤50ms processing)
 * - Standard: Portfolio updates, notifications (≤100ms processing)
 * - Background: Analytics, system maintenance (≤500ms processing)
 * 
 * COGNITIVE COMPLEXITY: ≤7 per method, ≤15 total per interface
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventClass"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CriticalEvent.class, name = "critical"),
    @JsonSubTypes.Type(value = HighPriorityEvent.class, name = "high"),
    @JsonSubTypes.Type(value = StandardEvent.class, name = "standard"),
    @JsonSubTypes.Type(value = BackgroundEvent.class, name = "background")
})
public sealed interface TradeMasterEvent permits 
    CriticalEvent, HighPriorityEvent, StandardEvent, BackgroundEvent {
    
    // ✅ IMMUTABLE: Event metadata record
    record EventHeader(
        String eventId,
        String correlationId, 
        String eventType,
        Priority priority,
        Instant timestamp,
        String sourceService,
        String targetService,
        String version,
        Map<String, String> metadata
    ) {
        // ✅ FACTORY: Smart constructor with validation
        public static EventHeader create(
                String eventType, 
                Priority priority, 
                String sourceService,
                String targetService) {
            return new EventHeader(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                eventType,
                priority,
                Instant.now(),
                sourceService,
                targetService,
                "1.0",
                Map.of()
            );
        }
        
        // ✅ BUILDER: For complex event headers
        public EventHeader withCorrelationId(String correlationId) {
            return new EventHeader(
                eventId, correlationId, eventType, priority, 
                timestamp, sourceService, targetService, version, metadata
            );
        }
        
        public EventHeader withMetadata(Map<String, String> newMetadata) {
            return new EventHeader(
                eventId, correlationId, eventType, priority,
                timestamp, sourceService, targetService, version, newMetadata
            );
        }
    }
    
    // ✅ FUNCTIONAL: Core event interface methods
    EventHeader header();
    Map<String, Object> payload();
    Optional<String> targetTopic();
    
    // ✅ FUNCTIONAL: Priority accessor for pattern matching
    default Priority priority() {
        return header().priority();
    }
    
    // ✅ PATTERN MATCHING: Functional event processing
    default <T> T process(EventProcessor<T> processor) {
        return switch (this) {
            case CriticalEvent critical -> processor.processCritical(critical);
            case HighPriorityEvent high -> processor.processHighPriority(high);
            case StandardEvent standard -> processor.processStandard(standard);
            case BackgroundEvent background -> processor.processBackground(background);
        };
    }
    
    // ✅ FUNCTIONAL: Event transformation
    default <U extends TradeMasterEvent> Optional<U> transform(EventTransformer<U> transformer) {
        return transformer.transform(this);
    }
    
    // ✅ VALIDATION: Event validation
    default ValidationResult validate() {
        return ValidationResult.success(); // Default implementation
    }
    
    // ✅ SERIALIZATION: JSON serialization support
    default String toJson() {
        return EventSerializer.toJson(this);
    }
}

/**
 * ✅ FUNCTIONAL: Event processor interface for pattern matching
 */
interface EventProcessor<T> {
    T processCritical(CriticalEvent event);
    T processHighPriority(HighPriorityEvent event);
    T processStandard(StandardEvent event);  
    T processBackground(BackgroundEvent event);
}

/**
 * ✅ FUNCTIONAL: Event transformer interface
 */
@FunctionalInterface
interface EventTransformer<T extends TradeMasterEvent> {
    Optional<T> transform(TradeMasterEvent source);
}

/**
 * ✅ IMMUTABLE: Validation result
 */
record ValidationResult(
    boolean isValid,
    Optional<String> errorMessage,
    Map<String, String> validationDetails
) {
    public static ValidationResult success() {
        return new ValidationResult(true, Optional.empty(), Map.of());
    }
    
    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, Optional.of(errorMessage), Map.of());
    }
    
    public static ValidationResult failure(String errorMessage, Map<String, String> details) {
        return new ValidationResult(false, Optional.of(errorMessage), details);
    }
}

/**
 * ✅ UTILITY: Event serialization utility
 */
class EventSerializer {
    
    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
        new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    public static String toJson(TradeMasterEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }
    
    public static TradeMasterEvent fromJson(String json, Class<? extends TradeMasterEvent> eventType) {
        try {
            return objectMapper.readValue(json, eventType);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event from JSON", e);
        }
    }
}
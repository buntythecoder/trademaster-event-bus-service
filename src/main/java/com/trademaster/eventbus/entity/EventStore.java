package com.trademaster.eventbus.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event Store Entity for Event Bus Service
 * 
 * MANDATORY: JPA Entity with Immutable Design
 * MANDATORY: Record Pattern Usage for Data Integrity
 * MANDATORY: Database Persistence Layer
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "event_store", 
       indexes = {
           @Index(name = "idx_event_correlation_id", columnList = "correlation_id"),
           @Index(name = "idx_event_priority", columnList = "priority"),
           @Index(name = "idx_event_timestamp", columnList = "created_at"),
           @Index(name = "idx_event_source", columnList = "source_service"),
           @Index(name = "idx_event_type", columnList = "event_type"),
           @Index(name = "idx_event_status", columnList = "processing_status"),
           @Index(name = "idx_event_composite_priority_status", columnList = "priority, processing_status"),
           @Index(name = "idx_event_composite_source_type", columnList = "source_service, event_type"),
           @Index(name = "idx_event_composite_status_timestamp", columnList = "processing_status, created_at"),
           @Index(name = "idx_event_retry_status", columnList = "retry_count, processing_status"),
           @Index(name = "idx_event_processing_time", columnList = "processing_start_time, processing_end_time")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 1, max = 100)
    private String correlationId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 1, max = 100)
    private String eventType;
    
    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventPriority priority;
    
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;
    
    @Column(name = "target_audience", length = 100)
    private String targetAudience;
    
    @Column(name = "event_payload", columnDefinition = "TEXT")
    private String eventPayload;
    
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;
    
    @Column(name = "processing_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
    
    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(10)
    private Integer retryCount = 0;
    
    @Builder.Default
    @Column(name = "max_retries", nullable = false)
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(10)
    private Integer maxRetries = 3;
    
    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;
    
    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (processingStatus == null) {
            processingStatus = ProcessingStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Event Priority Enumeration
     */
    public enum EventPriority {
        CRITICAL,    // Sub-25ms SLA
        HIGH,        // Sub-50ms SLA  
        STANDARD,    // Sub-100ms SLA
        BACKGROUND   // Sub-500ms SLA
    }
    
    /**
     * Processing Status Enumeration
     */
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRY,
        DEAD_LETTER
    }
}
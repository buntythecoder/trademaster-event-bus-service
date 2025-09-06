package com.trademaster.eventbus.repository;

import com.trademaster.eventbus.entity.EventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Store Repository for Event Bus Service
 * 
 * MANDATORY: JPA Repository Interface
 * MANDATORY: Functional Query Methods
 * MANDATORY: Database Access Layer
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, UUID> {
    
    /**
     * Find events by correlation ID for event tracing
     */
    List<EventStore> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
    
    /**
     * Find events by processing status
     */
    List<EventStore> findByProcessingStatusOrderByCreatedAtAsc(EventStore.ProcessingStatus status);
    
    /**
     * Find events by priority and status for processing queue
     */
    @Query("SELECT e FROM EventStore e WHERE e.priority = :priority AND e.processingStatus = :status ORDER BY e.createdAt ASC")
    List<EventStore> findByPriorityAndStatusOrderByCreatedAt(@Param("priority") EventStore.EventPriority priority, 
                                                              @Param("status") EventStore.ProcessingStatus status);
    
    /**
     * Find failed events eligible for retry
     */
    @Query("SELECT e FROM EventStore e WHERE e.processingStatus = :status AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC")
    List<EventStore> findFailedEventsForRetry(@Param("status") EventStore.ProcessingStatus status);
    
    /**
     * Find events by source service
     */
    List<EventStore> findBySourceServiceOrderByCreatedAtDesc(String sourceService);
    
    /**
     * Find events by target audience
     */
    List<EventStore> findByTargetAudienceOrderByCreatedAtDesc(String targetAudience);
    
    /**
     * Find events by event type
     */
    List<EventStore> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    /**
     * Find events within time range
     */
    @Query("SELECT e FROM EventStore e WHERE e.createdAt BETWEEN :startTime AND :endTime ORDER BY e.createdAt DESC")
    List<EventStore> findEventsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find slow processing events (exceeding SLA)
     */
    @Query("SELECT e FROM EventStore e WHERE e.processingStatus = 'PROCESSING' AND e.processingStartTime < :slaThreshold")
    List<EventStore> findSlowProcessingEvents(@Param("slaThreshold") LocalDateTime slaThreshold);
    
    /**
     * Count events by status
     */
    long countByProcessingStatus(EventStore.ProcessingStatus status);
    
    /**
     * Count events by priority
     */
    long countByPriority(EventStore.EventPriority priority);
    
    /**
     * Delete old completed events (for retention policy)
     */
    @Query("DELETE FROM EventStore e WHERE e.processingStatus = 'COMPLETED' AND e.createdAt < :retentionDate")
    int deleteCompletedEventsBefore(@Param("retentionDate") LocalDateTime retentionDate);
    
    /**
     * Find events for specific user/session
     */
    @Query("SELECT e FROM EventStore e WHERE e.targetAudience = :userId OR e.correlationId LIKE :sessionPattern ORDER BY e.createdAt DESC")
    List<EventStore> findEventsForUser(@Param("userId") String userId, @Param("sessionPattern") String sessionPattern);
}
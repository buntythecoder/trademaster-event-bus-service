package com.trademaster.eventbus.service;

import com.trademaster.eventbus.dto.EventRequest;
import com.trademaster.eventbus.dto.EventResponse;

import java.util.List;
import java.util.Map;

/**
 * Event Bus Service Interface
 * 
 * Core service interface for event bus operations.
 * Provides synchronous API for internal service-to-service communication.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface EventBusService {
    
    /**
     * Publish a single event
     */
    EventResponse publishEvent(EventRequest request);
    
    /**
     * Batch publish multiple events
     */
    List<EventResponse> batchPublishEvents(List<EventRequest> requests);
    
    /**
     * Get event history with filters
     */
    List<EventResponse> getEventHistory(String eventType, String sourceService, 
                                      String correlationId, int limit, int hoursBack);
    
    /**
     * Get event statistics
     */
    Map<String, Object> getEventStatistics(int hoursBack);
    
    /**
     * Acknowledge event processing
     */
    void acknowledgeEvent(String eventId, Map<String, Object> acknowledgment);
    
    /**
     * Check if Kafka is connected
     */
    boolean isKafkaConnected();
    
    /**
     * Check if Redis is connected
     */
    boolean isRedisConnected();
    
    /**
     * Get service metrics
     */
    Map<String, Object> getServiceMetrics();
}
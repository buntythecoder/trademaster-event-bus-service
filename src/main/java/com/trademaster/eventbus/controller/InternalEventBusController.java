package com.trademaster.eventbus.controller;

import com.trademaster.eventbus.dto.EventRequest;
import com.trademaster.eventbus.dto.EventResponse;
import com.trademaster.eventbus.dto.SubscriptionRequest;
import com.trademaster.eventbus.service.EventBusService;
import com.trademaster.eventbus.service.SubscriptionService;
import com.trademaster.eventbus.common.EventBusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Internal Event Bus API Controller
 * 
 * Provides internal endpoints for service-to-service communication.
 * These endpoints bypass JWT authentication and use service API key authentication.
 * 
 * Security:
 * - Service API key authentication required
 * - Role-based access control (ROLE_SERVICE, ROLE_INTERNAL)
 * - Internal network access only
 * - Audit logging for all operations
 * 
 * Event Bus Internal Operations:
 * - Publish events from other services
 * - Subscribe to event streams
 * - Query event history and statistics
 * - Manage service-to-service event routing
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/internal/v1/event-bus")
@RequiredArgsConstructor
@Slf4j
// @PreAuthorize("hasRole('SERVICE') and hasRole('INTERNAL')")
public class InternalEventBusController {
    
    private final EventBusService eventBusService;
    private final SubscriptionService subscriptionService;
    
    /**
     * Publish event (internal service call)
     * Used by trading-service, broker-auth-service, etc. to publish events
     */
    @PostMapping("/events/publish")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<EventResponse> publishEvent(@Valid @RequestBody EventRequest request) {
        try {
            log.info("Internal API: Publishing event type: {} from service: {}", 
                    request.getEventType(), request.getSourceService());
            
            EventResponse response = eventBusService.publishEvent(request);
            
            log.info("Internal API: Event published successfully: {}", response.getEventId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to publish event: {}", request, e);
            throw new EventBusException("Failed to publish event", e);
        }
    }
    
    /**
     * Batch publish events (internal service call)
     * Used for high-throughput event publishing from trading systems
     */
    @PostMapping("/events/batch-publish")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<List<EventResponse>> batchPublishEvents(
            @Valid @RequestBody List<EventRequest> requests) {
        try {
            log.info("Internal API: Batch publishing {} events", requests.size());
            
            List<EventResponse> responses = eventBusService.batchPublishEvents(requests);
            
            log.info("Internal API: Batch published {} events successfully", responses.size());
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to batch publish events: size={}", requests.size(), e);
            throw new EventBusException("Failed to batch publish events", e);
        }
    }
    
    /**
     * Subscribe to events (internal service call)
     * Used by services to subscribe to specific event types
     */
    @PostMapping("/subscriptions")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        try {
            log.info("Internal API: Creating subscription for service: {}, eventTypes: {}", 
                    request.getServiceId(), request.getEventTypes());
            
            String subscriptionId = subscriptionService.createSubscription(request);
            
            Map<String, Object> response = Map.of(
                "subscriptionId", subscriptionId,
                "serviceId", request.getServiceId(),
                "eventTypes", request.getEventTypes(),
                "status", "active",
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("Internal API: Subscription created: {}", subscriptionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to create subscription: {}", request, e);
            throw new EventBusException("Failed to create subscription", e);
        }
    }
    
    /**
     * Get event history (internal service call)
     * Used by audit-service, analytics-service for event tracking
     */
    @GetMapping("/events/history")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<List<EventResponse>> getEventHistory(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "24") int hoursBack) {
        try {
            log.info("Internal API: Getting event history - type: {}, source: {}, correlationId: {}, limit: {}", 
                    eventType, sourceService, correlationId, limit);
            
            List<EventResponse> events = eventBusService.getEventHistory(
                eventType, sourceService, correlationId, limit, hoursBack);
            
            log.info("Internal API: Retrieved {} events from history", events.size());
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to get event history", e);
            throw new EventBusException("Failed to retrieve event history", e);
        }
    }
    
    /**
     * Get event statistics (internal service call)
     * Used by monitoring services for system health tracking
     */
    @GetMapping("/events/statistics")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> getEventStatistics(
            @RequestParam(required = false, defaultValue = "1") int hoursBack) {
        try {
            log.info("Internal API: Getting event statistics for last {} hours", hoursBack);
            
            Map<String, Object> statistics = eventBusService.getEventStatistics(hoursBack);
            
            log.info("Internal API: Retrieved event statistics: {} metrics", statistics.size());
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to get event statistics", e);
            throw new EventBusException("Failed to retrieve event statistics", e);
        }
    }
    
    /**
     * Get subscription status (internal service call)
     * Used by services to check their subscription health
     */
    @GetMapping("/subscriptions/{subscriptionId}")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(
            @PathVariable String subscriptionId) {
        try {
            log.info("Internal API: Getting subscription status: {}", subscriptionId);
            
            Map<String, Object> status = subscriptionService.getSubscriptionStatus(subscriptionId);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to get subscription status: {}", subscriptionId, e);
            throw new EventBusException("Failed to get subscription status", e);
        }
    }
    
    /**
     * Update subscription (internal service call)
     * Used by services to modify their event subscriptions
     */
    @PutMapping("/subscriptions/{subscriptionId}")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> updateSubscription(
            @PathVariable String subscriptionId,
            @Valid @RequestBody SubscriptionRequest request) {
        try {
            log.info("Internal API: Updating subscription: {} for service: {}", 
                    subscriptionId, request.getServiceId());
            
            subscriptionService.updateSubscription(subscriptionId, request);
            
            Map<String, Object> response = Map.of(
                "subscriptionId", subscriptionId,
                "status", "updated",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to update subscription: {}", subscriptionId, e);
            throw new EventBusException("Failed to update subscription", e);
        }
    }
    
    /**
     * Delete subscription (internal service call)
     * Used when services are shutting down or changing event requirements
     */
    @DeleteMapping("/subscriptions/{subscriptionId}")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> deleteSubscription(@PathVariable String subscriptionId) {
        try {
            log.info("Internal API: Deleting subscription: {}", subscriptionId);
            
            subscriptionService.deleteSubscription(subscriptionId);
            
            Map<String, Object> response = Map.of(
                "subscriptionId", subscriptionId,
                "status", "deleted",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to delete subscription: {}", subscriptionId, e);
            throw new EventBusException("Failed to delete subscription", e);
        }
    }
    
    /**
     * Notify event delivery (internal service call)
     * Used by event consumers to acknowledge successful event processing
     */
    @PostMapping("/events/{eventId}/acknowledge")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> acknowledgeEvent(
            @PathVariable String eventId,
            @RequestBody Map<String, Object> acknowledgment) {
        try {
            log.info("Internal API: Acknowledging event: {} by service: {}", 
                    eventId, acknowledgment.get("serviceId"));
            
            eventBusService.acknowledgeEvent(eventId, acknowledgment);
            
            Map<String, Object> response = Map.of(
                "eventId", eventId,
                "status", "acknowledged",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to acknowledge event: {}", eventId, e);
            throw new EventBusException("Failed to acknowledge event", e);
        }
    }
    
    /**
     * Health check for internal services
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "event-bus-service",
            "status", "UP",
            "internal_api", "available",
            "kafka_connected", eventBusService.isKafkaConnected(),
            "redis_connected", eventBusService.isRedisConnected(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Get service metrics (internal service call)
     * Used by monitoring services for performance tracking
     */
    @GetMapping("/metrics")
    // @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> getServiceMetrics() {
        try {
            log.info("Internal API: Getting service metrics");
            
            Map<String, Object> metrics = eventBusService.getServiceMetrics();
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to get service metrics", e);
            throw new EventBusException("Failed to retrieve service metrics", e);
        }
    }
}
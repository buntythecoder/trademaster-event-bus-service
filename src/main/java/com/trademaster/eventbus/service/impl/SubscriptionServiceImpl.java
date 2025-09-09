package com.trademaster.eventbus.service.impl;

import com.trademaster.eventbus.dto.SubscriptionRequest;
import com.trademaster.eventbus.service.SubscriptionService;
import com.trademaster.eventbus.service.EventSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Subscription Service Implementation
 * 
 * Adapts the existing EventSubscriptionService to provide synchronous API
 * for internal service-to-service communication.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    
    private final EventSubscriptionService eventSubscriptionService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public String createSubscription(SubscriptionRequest request) {
        try {
            log.info("Creating subscription for service: {} with event types: {}", 
                    request.getServiceId(), request.getEventTypes());
            
            // Generate subscription ID
            String subscriptionId = "SUB_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            
            // Store subscription details in Redis
            Map<String, Object> subscriptionData = new java.util.HashMap<>();
            subscriptionData.put("subscriptionId", subscriptionId);
            subscriptionData.put("serviceId", request.getServiceId());
            subscriptionData.put("eventTypes", request.getEventTypes());
            subscriptionData.put("callbackUrl", request.getCallbackUrl() != null ? request.getCallbackUrl() : "");
            subscriptionData.put("filters", request.getFilters() != null ? request.getFilters() : Map.of());
            subscriptionData.put("priority", request.getPriority() != null ? request.getPriority() : "STANDARD");
            subscriptionData.put("batchDelivery", request.getBatchDelivery() != null ? request.getBatchDelivery() : false);
            subscriptionData.put("batchSize", request.getBatchSize() != null ? request.getBatchSize() : 10);
            subscriptionData.put("maxRetries", request.getMaxRetries() != null ? request.getMaxRetries() : 3);
            subscriptionData.put("active", request.getActive() != null ? request.getActive() : true);
            subscriptionData.put("createdAt", LocalDateTime.now());
            subscriptionData.put("status", "active");
            
            String subscriptionKey = "subscription:" + subscriptionId;
            redisTemplate.opsForHash().putAll(subscriptionKey, subscriptionData);
            redisTemplate.expire(subscriptionKey, 30, TimeUnit.DAYS); // 30 days expiration
            
            // Also maintain a service-to-subscriptions mapping
            String serviceKey = "service:subscriptions:" + request.getServiceId();
            redisTemplate.opsForSet().add(serviceKey, subscriptionId);
            redisTemplate.expire(serviceKey, 30, TimeUnit.DAYS);
            
            log.info("Subscription created successfully: {}", subscriptionId);
            return subscriptionId;
            
        } catch (Exception e) {
            log.error("Error creating subscription: {}", request, e);
            throw new RuntimeException("Failed to create subscription", e);
        }
    }
    
    @Override
    public Map<String, Object> getSubscriptionStatus(String subscriptionId) {
        try {
            log.info("Getting subscription status: {}", subscriptionId);
            
            String subscriptionKey = "subscription:" + subscriptionId;
            Map<Object, Object> subscriptionData = redisTemplate.opsForHash().entries(subscriptionKey);
            
            if (subscriptionData.isEmpty()) {
                return Map.of(
                    "subscriptionId", subscriptionId,
                    "status", "not_found",
                    "error", "Subscription not found",
                    "timestamp", LocalDateTime.now()
                );
            }
            
            // Convert to String keys for consistency
            return subscriptionData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().toString(),
                    entry -> entry.getValue()
                ));
                
        } catch (Exception e) {
            log.error("Error getting subscription status: {}", subscriptionId, e);
            return Map.of(
                "subscriptionId", subscriptionId,
                "status", "error",
                "error", "Failed to retrieve subscription: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    @Override
    public void updateSubscription(String subscriptionId, SubscriptionRequest request) {
        try {
            log.info("Updating subscription: {} for service: {}", subscriptionId, request.getServiceId());
            
            String subscriptionKey = "subscription:" + subscriptionId;
            
            // Check if subscription exists
            if (!redisTemplate.hasKey(subscriptionKey)) {
                throw new RuntimeException("Subscription not found: " + subscriptionId);
            }
            
            // Update subscription details
            Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("eventTypes", request.getEventTypes());
            updates.put("callbackUrl", request.getCallbackUrl() != null ? request.getCallbackUrl() : "");
            updates.put("filters", request.getFilters() != null ? request.getFilters() : Map.of());
            updates.put("priority", request.getPriority() != null ? request.getPriority() : "STANDARD");
            updates.put("batchDelivery", request.getBatchDelivery() != null ? request.getBatchDelivery() : false);
            updates.put("batchSize", request.getBatchSize() != null ? request.getBatchSize() : 10);
            updates.put("maxRetries", request.getMaxRetries() != null ? request.getMaxRetries() : 3);
            updates.put("active", request.getActive() != null ? request.getActive() : true);
            updates.put("updatedAt", LocalDateTime.now());
            
            redisTemplate.opsForHash().putAll(subscriptionKey, updates);
            
            log.info("Subscription updated successfully: {}", subscriptionId);
            
        } catch (Exception e) {
            log.error("Error updating subscription: {}", subscriptionId, e);
            throw new RuntimeException("Failed to update subscription", e);
        }
    }
    
    @Override
    public void deleteSubscription(String subscriptionId) {
        try {
            log.info("Deleting subscription: {}", subscriptionId);
            
            String subscriptionKey = "subscription:" + subscriptionId;
            
            // Get subscription details first to remove from service mapping
            Map<Object, Object> subscriptionData = redisTemplate.opsForHash().entries(subscriptionKey);
            
            if (!subscriptionData.isEmpty()) {
                String serviceId = (String) subscriptionData.get("serviceId");
                if (serviceId != null) {
                    String serviceKey = "service:subscriptions:" + serviceId;
                    redisTemplate.opsForSet().remove(serviceKey, subscriptionId);
                }
            }
            
            // Delete the subscription
            redisTemplate.delete(subscriptionKey);
            
            log.info("Subscription deleted successfully: {}", subscriptionId);
            
        } catch (Exception e) {
            log.error("Error deleting subscription: {}", subscriptionId, e);
            throw new RuntimeException("Failed to delete subscription", e);
        }
    }
}
package com.trademaster.eventbus.service;

import com.trademaster.eventbus.dto.SubscriptionRequest;

import java.util.Map;

/**
 * Subscription Service Interface
 * 
 * Core service interface for event subscription operations.
 * Provides synchronous API for managing event subscriptions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface SubscriptionService {
    
    /**
     * Create a new subscription
     */
    String createSubscription(SubscriptionRequest request);
    
    /**
     * Get subscription status
     */
    Map<String, Object> getSubscriptionStatus(String subscriptionId);
    
    /**
     * Update an existing subscription
     */
    void updateSubscription(String subscriptionId, SubscriptionRequest request);
    
    /**
     * Delete a subscription
     */
    void deleteSubscription(String subscriptionId);
}
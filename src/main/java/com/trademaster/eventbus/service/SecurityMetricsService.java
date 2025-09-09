package com.trademaster.eventbus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ✅ SECURITY METRICS SERVICE: Security metrics collection and monitoring
 */
@Service
@Slf4j
public class SecurityMetricsService {
    
    private final AtomicLong tokenRefreshCount = new AtomicLong(0);
    private final AtomicLong authenticationAttempts = new AtomicLong(0);
    private final AtomicLong authenticationFailures = new AtomicLong(0);
    
    /**
     * ✅ FUNCTIONAL: Record token refresh event
     */
    public void recordTokenRefresh(String userId) {
        tokenRefreshCount.incrementAndGet();
        log.debug("Token refresh recorded for user: {}", userId);
    }
    
    /**
     * ✅ FUNCTIONAL: Record authentication attempt
     */
    public void recordAuthenticationAttempt(String userId) {
        authenticationAttempts.incrementAndGet();
        log.debug("Authentication attempt recorded for user: {}", userId);
    }
    
    /**
     * ✅ FUNCTIONAL: Record authentication failure
     */
    public void recordAuthenticationFailure(String userId) {
        authenticationFailures.incrementAndGet();
        log.debug("Authentication failure recorded for user: {}", userId);
    }
    
    /**
     * ✅ FUNCTIONAL: Get token refresh count
     */
    public long getTokenRefreshCount() {
        return tokenRefreshCount.get();
    }
    
    /**
     * ✅ FUNCTIONAL: Get authentication attempts count
     */
    public long getAuthenticationAttempts() {
        return authenticationAttempts.get();
    }
    
    /**
     * ✅ FUNCTIONAL: Get authentication failures count
     */
    public long getAuthenticationFailures() {
        return authenticationFailures.get();
    }
    
    /**
     * ✅ FUNCTIONAL: Record authentication error
     */
    public void recordAuthenticationError(String errorType) {
        authenticationFailures.incrementAndGet();
        log.debug("Authentication error recorded: {}", errorType);
    }
    
    /**
     * ✅ FUNCTIONAL: Record cache size metric
     */
    public void recordCacheSize(String cacheName, int size) {
        log.debug("Cache size recorded for {}: {}", cacheName, size);
    }
    
    /**
     * ✅ FUNCTIONAL: Record security event
     */
    public void recordSecurityEvent(String eventType) {
        log.debug("Security event recorded: {}", eventType);
    }
    
    /**
     * ✅ FUNCTIONAL: Record authorization attempt
     */
    public void recordAuthorizationAttempt(String result, int policiesCount) {
        log.debug("Authorization attempt recorded: result={}, policies={}", result, policiesCount);
    }
}
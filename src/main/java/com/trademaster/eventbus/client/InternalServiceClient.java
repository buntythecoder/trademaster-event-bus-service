package com.trademaster.eventbus.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Internal Service Client for Event Bus Service
 * 
 * Handles service-to-service communication with API key authentication.
 * This demonstrates how event-bus-service calls other internal services.
 * 
 * Usage Examples:
 * - Call trading-service to get order updates
 * - Call notification-service to send event-based notifications
 * - Call risk-service to publish risk alerts
 * - Call audit-service to log critical events
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class InternalServiceClient {
    
    private final RestTemplate restTemplate;
    private final RestTemplate circuitBreakerRestTemplate;
    
    @Value("${trademaster.security.service.api-key}")
    private String serviceApiKey;
    
    @Value("${trademaster.service.name:event-bus-service}")
    private String serviceName;
    
    @Value("${trademaster.services.trading-service.url:http://trading-service:8083}")
    private String tradingServiceUrl;
    
    @Value("${trademaster.services.notification-service.url:http://notification-service:8084}")
    private String notificationServiceUrl;
    
    @Value("${trademaster.services.risk-service.url:http://risk-service:8085}")
    private String riskServiceUrl;
    
    /**
     * ✅ CONSTRUCTOR INJECTION: Use configured RestTemplate with connection pooling
     * Primary RestTemplate has connection pooling configured via HttpClientConfiguration
     * Circuit breaker RestTemplate provides additional resilience for external calls
     */
    public InternalServiceClient(RestTemplate restTemplate, 
                               @Qualifier("circuitBreakerRestTemplate") RestTemplate circuitBreakerRestTemplate) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRestTemplate = circuitBreakerRestTemplate;
        log.info("✅ InternalServiceClient initialized with connection pooling");
    }
    
    /**
     * Call trading service internal API
     * Example: Get user positions when processing position-related events
     */
    public Map<String, Object> getTradingServiceData(String endpoint, Object payload) {
        try {
            String url = tradingServiceUrl + "/api/internal/v1/trading" + endpoint;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            
            log.info("Calling trading service internal API: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, payload != null ? HttpMethod.POST : HttpMethod.GET, entity, Map.class);
            
            log.info("Trading service API call successful: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Trading service API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Trading service call failed", e);
        }
    }
    
    /**
     * Get user positions from trading service
     * Used when processing position-change events
     */
    public Map<String, Object> getUserPositions(String userId) {
        try {
            String url = tradingServiceUrl + "/api/internal/v1/trading/positions/" + userId;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            log.info("Retrieved user positions for user: {}", userId);
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to get user positions from trading service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user positions", e);
        }
    }
    
    /**
     * Get order details from trading service
     * Used when processing order-related events
     */
    public Map<String, Object> getOrderDetails(String orderId) {
        try {
            String url = tradingServiceUrl + "/api/internal/v1/trading/orders/" + orderId;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            log.info("Retrieved order details for order: {}", orderId);
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to get order details from trading service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get order details", e);
        }
    }
    
    /**
     * Send notification through notification service
     * Used when events need to trigger user notifications
     */
    public Map<String, Object> sendNotification(Map<String, Object> notificationRequest) {
        try {
            String url = notificationServiceUrl + "/api/internal/v1/notifications/send";
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(notificationRequest, headers);
            
            log.info("Sending notification: {}", notificationRequest.get("type"));
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            log.info("Notification sent successfully");
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }
    
    /**
     * Publish risk alert to risk service
     * Used when critical events indicate risk threshold breaches
     */
    public Map<String, Object> publishRiskAlert(Map<String, Object> riskAlert) {
        try {
            String url = riskServiceUrl + "/api/internal/v1/risk/alerts";
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(riskAlert, headers);
            
            log.info("Publishing risk alert: {}", riskAlert.get("alertType"));
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            log.info("Risk alert published successfully");
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to publish risk alert: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish risk alert", e);
        }
    }
    
    /**
     * Async call to any internal service
     * Used for non-blocking service-to-service communication
     */
    public CompletableFuture<Map<String, Object>> callServiceAsync(
            String serviceUrl, String endpoint, Object payload) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = serviceUrl + endpoint;
                
                HttpHeaders headers = createServiceHeaders();
                HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
                
                log.info("Async call to service: {}", url);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, payload != null ? HttpMethod.POST : HttpMethod.GET, entity, Map.class);
                
                log.info("Async service call successful: {}", response.getStatusCode());
                return response.getBody();
                
            } catch (RestClientException e) {
                log.error("Async service call failed: {}", e.getMessage(), e);
                throw new RuntimeException("Async service call failed", e);
            }
        });
    }
    
    /**
     * Create headers for service-to-service authentication
     */
    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-API-Key", serviceApiKey);
        headers.set("X-Service-ID", serviceName);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        // Add correlation ID for tracing
        String correlationId = "EVB-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        headers.set("X-Correlation-ID", correlationId);
        
        return headers;
    }
    
    /**
     * Health check for any internal service
     */
    public boolean checkServiceHealth(String serviceUrl) {
        try {
            String url = serviceUrl + "/api/internal/v1/*/health";
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("Service health check failed for: {}", serviceUrl, e);
            return false;
        }
    }
    
    /**
     * Check trading service health specifically
     */
    public boolean checkTradingServiceHealth() {
        return checkServiceHealth(tradingServiceUrl + "/api/internal/v1/trading/health");
    }
    
    /**
     * Batch health check for all configured services
     */
    public Map<String, Boolean> checkAllServicesHealth() {
        return Map.of(
            "trading-service", checkTradingServiceHealth(),
            "notification-service", checkServiceHealth(notificationServiceUrl + "/api/internal/v1/notifications/health"),
            "risk-service", checkServiceHealth(riskServiceUrl + "/api/internal/v1/risk/health")
        );
    }
}
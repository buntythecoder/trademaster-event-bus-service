package com.trademaster.eventbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Subscription Request DTO
 * 
 * Data transfer object for event subscription requests.
 * Used by internal services to subscribe to specific event types.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    
    @NotBlank(message = "Service ID is required")
    private String serviceId;
    
    @NotEmpty(message = "At least one event type is required")
    private List<String> eventTypes;
    
    private String callbackUrl;
    
    private String webhookSecret;
    
    private Map<String, String> filters; // Key-value filters for events
    
    private String priority; // Subscribe to events with this priority or higher
    
    private Boolean batchDelivery; // Whether to receive events in batches
    
    private Integer batchSize; // Maximum batch size
    
    private Integer batchTimeoutSeconds; // Maximum wait time for batch
    
    private Integer maxRetries; // Maximum retry attempts for failed deliveries
    
    private Integer retryDelaySeconds; // Delay between retry attempts
    
    private LocalDateTime expiresAt; // When subscription expires
    
    private Boolean active; // Whether subscription is active
    
    private Map<String, Object> metadata; // Additional subscription metadata
}
package com.trademaster.eventbus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event Response DTO
 * 
 * Data transfer object for event publication and retrieval responses.
 * Contains event details and processing status information.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    
    private String eventId;
    
    private String eventType;
    
    private String sourceService;
    
    private String userId;
    
    private String correlationId;
    
    private Map<String, Object> payload;
    
    private String priority;
    
    private LocalDateTime timestamp;
    
    private LocalDateTime publishedAt;
    
    private String status; // PUBLISHED, DELIVERED, FAILED, ACKNOWLEDGED
    
    private Map<String, String> headers;
    
    private String targetService;
    
    private Integer retryCount;
    
    private LocalDateTime expirationTime;
    
    private Long processingTimeMs;
    
    private String errorMessage;
    
    private Integer deliveryCount;
    
    private LocalDateTime lastDeliveryAttempt;
}
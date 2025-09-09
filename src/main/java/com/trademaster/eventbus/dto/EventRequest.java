package com.trademaster.eventbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event Request DTO
 * 
 * Data transfer object for incoming event publication requests.
 * Used by internal services to publish events through the event bus.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    @NotBlank(message = "Source service is required")
    private String sourceService;
    
    private String userId;
    
    private String correlationId;
    
    @NotNull(message = "Event payload is required")
    private Map<String, Object> payload;
    
    private String priority; // CRITICAL, HIGH, STANDARD, BACKGROUND
    
    private LocalDateTime timestamp;
    
    private Map<String, String> headers;
    
    private String targetService; // Optional: specific target service
    
    private Integer retryCount;
    
    private LocalDateTime expirationTime;
}
package com.trademaster.eventbus.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Internal Greeting Controller for Event Bus Service
 * 
 * Used for testing internal API authentication with Kong dynamic API keys.
 * This endpoint requires SERVICE role authentication via ServiceApiKeyFilter.
 */
@RestController
@RequestMapping("/api/internal")
@Slf4j
public class InternalGreetingController {
    
    /**
     * Internal greeting endpoint for testing API key authentication
     * Requires SERVICE role authentication through Kong dynamic keys
     */
    @GetMapping("/greeting")
    @PreAuthorize("hasRole('SERVICE')")
    public Map<String, Object> getGreeting() {
        log.info("Internal greeting endpoint accessed");
        
        return Map.of(
            "message", "Hello from Event Bus Service Internal API!",
            "timestamp", LocalDateTime.now(),
            "service", "event-bus-service",
            "authenticated", true,
            "role", "SERVICE"
        );
    }
    
    /**
     * Internal status endpoint for service health validation
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('SERVICE')")
    public Map<String, Object> getStatus() {
        log.info("Internal status endpoint accessed");
        
        return Map.of(
            "status", "UP",
            "service", "event-bus-service",
            "timestamp", LocalDateTime.now(),
            "authenticated", true,
            "message", "Service is running and authenticated"
        );
    }
}
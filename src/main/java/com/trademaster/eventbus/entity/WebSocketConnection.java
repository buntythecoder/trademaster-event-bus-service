package com.trademaster.eventbus.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket Connection Entity for Event Bus Service
 * 
 * MANDATORY: JPA Entity for Connection Management
 * MANDATORY: Database Persistence Layer
 * MANDATORY: Connection State Tracking
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "websocket_connections",
       indexes = {
           @Index(name = "idx_ws_user_id", columnList = "user_id"),
           @Index(name = "idx_ws_session_id", columnList = "session_id"),
           @Index(name = "idx_ws_status", columnList = "connection_status"),
           @Index(name = "idx_ws_created_at", columnList = "created_at"),
           @Index(name = "idx_ws_last_activity", columnList = "last_activity_at"),
           @Index(name = "idx_ws_composite_user_status", columnList = "user_id, connection_status"),
           @Index(name = "idx_ws_composite_status_activity", columnList = "connection_status, last_activity_at"),
           @Index(name = "idx_ws_active_connections", columnList = "connection_status, created_at"),
           @Index(name = "idx_ws_user_activity", columnList = "user_id, last_activity_at"),
           @Index(name = "idx_ws_performance_metrics", columnList = "messages_sent, messages_received, connection_duration_seconds")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "connection_id", updatable = false, nullable = false)
    private UUID connectionId;
    
    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min = 1, max = 100)
    private String sessionId;
    
    @Column(name = "user_id", length = 100)
    @jakarta.validation.constraints.Size(max = 100)
    private String userId;
    
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "connection_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConnectionStatus connectionStatus;
    
    @Column(name = "subscription_topics", columnDefinition = "TEXT")
    private String subscriptionTopics;
    
    @Builder.Default
    @Column(name = "messages_sent", nullable = false)
    @jakarta.validation.constraints.Min(0)
    private Long messagesSent = 0L;
    
    @Builder.Default
    @Column(name = "messages_received", nullable = false)
    @jakarta.validation.constraints.Min(0) 
    private Long messagesReceived = 0L;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @Column(name = "connection_duration_seconds")
    private Long connectionDurationSeconds;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
        if (connectionStatus == null) {
            connectionStatus = ConnectionStatus.CONNECTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (connectionStatus == ConnectionStatus.DISCONNECTED && disconnectedAt == null) {
            disconnectedAt = LocalDateTime.now();
            if (createdAt != null) {
                connectionDurationSeconds = java.time.Duration.between(createdAt, disconnectedAt).getSeconds();
            }
        }
    }
    
    /**
     * Connection Status Enumeration
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        IDLE,
        ERROR
    }
}
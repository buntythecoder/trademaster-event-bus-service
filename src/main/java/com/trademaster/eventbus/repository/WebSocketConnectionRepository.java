package com.trademaster.eventbus.repository;

import com.trademaster.eventbus.entity.WebSocketConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WebSocket Connection Repository for Event Bus Service
 * 
 * MANDATORY: JPA Repository Interface
 * MANDATORY: Connection Management Queries
 * MANDATORY: Database Access Layer
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface WebSocketConnectionRepository extends JpaRepository<WebSocketConnection, UUID> {
    
    /**
     * Find connection by session ID
     */
    Optional<WebSocketConnection> findBySessionId(String sessionId);
    
    /**
     * Find active connections by user ID
     */
    List<WebSocketConnection> findByUserIdAndConnectionStatus(String userId, WebSocketConnection.ConnectionStatus status);
    
    /**
     * Find all active connections
     */
    List<WebSocketConnection> findByConnectionStatus(WebSocketConnection.ConnectionStatus status);
    
    /**
     * Find connections by client IP
     */
    List<WebSocketConnection> findByClientIpAndConnectionStatus(String clientIp, WebSocketConnection.ConnectionStatus status);
    
    /**
     * Find idle connections for cleanup
     */
    @Query("SELECT w FROM WebSocketConnection w WHERE w.connectionStatus = 'CONNECTED' AND w.lastActivityAt < :idleThreshold")
    List<WebSocketConnection> findIdleConnections(@Param("idleThreshold") LocalDateTime idleThreshold);
    
    /**
     * Find connections with topic subscriptions
     */
    @Query("SELECT w FROM WebSocketConnection w WHERE w.connectionStatus = 'CONNECTED' AND w.subscriptionTopics LIKE %:topic%")
    List<WebSocketConnection> findConnectionsSubscribedToTopic(@Param("topic") String topic);
    
    /**
     * Count active connections
     */
    long countByConnectionStatus(WebSocketConnection.ConnectionStatus status);
    
    /**
     * Count connections by user
     */
    long countByUserIdAndConnectionStatus(String userId, WebSocketConnection.ConnectionStatus status);
    
    /**
     * Find high-activity connections
     */
    @Query("SELECT w FROM WebSocketConnection w WHERE w.connectionStatus = 'CONNECTED' AND (w.messagesSent + w.messagesReceived) > :messageThreshold")
    List<WebSocketConnection> findHighActivityConnections(@Param("messageThreshold") Long messageThreshold);
    
    /**
     * Find connections within time range
     */
    @Query("SELECT w FROM WebSocketConnection w WHERE w.createdAt BETWEEN :startTime AND :endTime")
    List<WebSocketConnection> findConnectionsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get connection statistics
     */
    @Query("SELECT COUNT(w), AVG(w.connectionDurationSeconds), SUM(w.messagesSent + w.messagesReceived) FROM WebSocketConnection w WHERE w.connectionStatus = 'DISCONNECTED'")
    Object[] getConnectionStatistics();
    
    /**
     * Delete old disconnected connections (for retention policy)
     */
    @Query("DELETE FROM WebSocketConnection w WHERE w.connectionStatus = 'DISCONNECTED' AND w.disconnectedAt < :retentionDate")
    int deleteOldDisconnectedConnections(@Param("retentionDate") LocalDateTime retentionDate);
    
    /**
     * Find user's most recent connection
     */
    Optional<WebSocketConnection> findTopByUserIdOrderByCreatedAtDesc(String userId);
}
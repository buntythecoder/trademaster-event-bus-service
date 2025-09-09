package com.trademaster.eventbus.service;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * ✅ SINGLE RESPONSIBILITY: WebSocket Connection Lifecycle Management
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility for connection operations only
 * - Rule #3: Functional Programming - No if-else statements
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total per class
 * - Rule #12: Virtual Threads for all async operations
 * 
 * RESPONSIBILITIES:
 * - WebSocket session registration and cleanup
 * - Connection state management
 * - Connection health monitoring
 * - Session metadata tracking
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionHandler {
    
    // ✅ VIRTUAL THREADS: Dedicated executor for connection operations
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    // ✅ IMMUTABLE: Active connections registry
    private final ConcurrentHashMap<String, WebSocketConnection> activeConnections = 
        new ConcurrentHashMap<>();
    
    /**
     * ✅ FUNCTIONAL: Register new WebSocket connection
     * Cognitive Complexity: 2
     */
    public CompletableFuture<Result<WebSocketConnection, GatewayError>> registerConnection(
            WebSocketSession session, 
            SecurityAuthenticationService.AuthenticationResult authResult) {
        
        return CompletableFuture.supplyAsync(() -> 
            createWebSocketConnection(session, authResult)
                .map(this::storeConnection)
                .map(this::logConnectionEstablished), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Cleanup WebSocket connection
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Result<Void, GatewayError>> cleanupConnection(String sessionId) {
        return CompletableFuture.supplyAsync(() -> 
            java.util.Optional.ofNullable(activeConnections.remove(sessionId))
                .map(connection -> {
                    log.info("WebSocket connection cleaned up: session={}, user={}", 
                        sessionId, connection.auth().userDetails().userId());
                    return Result.<Void, GatewayError>success(null);
                })
                .orElse(Result.failure(new GatewayError.ValidationError.InvalidInput(
                    "Connection not found for cleanup",
                    "sessionId",
                    sessionId))), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Get active connection by session ID
     * Cognitive Complexity: 1
     */
    public Result<WebSocketConnection, GatewayError> getConnection(String sessionId) {
        return java.util.Optional.ofNullable(activeConnections.get(sessionId))
            .map(Result::<WebSocketConnection, GatewayError>success)
            .orElse(Result.failure(new GatewayError.ValidationError.InvalidInput(
                "WebSocket session not found",
                "sessionId",
                sessionId
            )));
    }
    
    /**
     * ✅ FUNCTIONAL: Get all active connections for event routing
     * Cognitive Complexity: 1
     */
    public CompletableFuture<Result<Map<String, WebSocketConnection>, GatewayError>> getActiveConnections() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, WebSocketConnection> connections = new java.util.HashMap<>(activeConnections);
            return Result.<Map<String, WebSocketConnection>, GatewayError>success(connections);
        }, virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Get all active connections for user
     * Cognitive Complexity: 2
     */
    public Set<WebSocketConnection> getUserConnections(String userId) {
        return activeConnections.values().stream()
            .filter(conn -> conn.auth().userDetails().userId().equals(userId))
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * ✅ FUNCTIONAL: Get connection count for monitoring
     * Cognitive Complexity: 1
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * ✅ FUNCTIONAL: Health check for all connections
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Map<String, Boolean>> checkConnectionHealth() {
        return CompletableFuture.supplyAsync(() -> 
            activeConnections.entrySet().stream()
                .collect(java.util.stream.Collectors.toConcurrentMap(
                    Map.Entry::getKey,
                    entry -> isConnectionHealthy(entry.getValue())
                )), 
            virtualThreadExecutor);
    }
    
    // ✅ PRIVATE HELPERS: Single responsibility helper methods
    
    /**
     * ✅ FUNCTIONAL: Create WebSocket connection record
     * Cognitive Complexity: 3
     */
    private Result<WebSocketConnection, GatewayError> createWebSocketConnection(
            WebSocketSession session, 
            SecurityAuthenticationService.AuthenticationResult authResult) {
        
        // ✅ FUNCTIONAL: Validate required parameters
        return java.util.Optional.ofNullable(session)
            .filter(s -> authResult != null)
            .map(s -> {
                try {
                    WebSocketConnection connection = new WebSocketConnection(
                        s.getId(),
                        s,
                        authResult,
                        Instant.now(),
                        java.util.Optional.empty(),
                        ConnectionStatus.ACTIVE
                    );
                    return Result.<WebSocketConnection, GatewayError>success(connection);
                } catch (Exception e) {
                    log.error("Failed to create WebSocket connection: {}", e.getMessage());
                    return Result.<WebSocketConnection, GatewayError>failure(
                        new GatewayError.SystemError.InternalServerError(
                            "Connection creation failed: " + e.getMessage(),
                            "conn-creation-" + System.currentTimeMillis()));
                }
            })
            .orElse(Result.failure(new GatewayError.ValidationError.InvalidInput(
                "Invalid session or authentication parameters",
                "session",
                "null_or_invalid")));
    }
    
    /**
     * ✅ FUNCTIONAL: Store connection in registry
     * Cognitive Complexity: 1
     */
    private WebSocketConnection storeConnection(WebSocketConnection connection) {
        activeConnections.put(connection.sessionId(), connection);
        return connection;
    }
    
    /**
     * ✅ FUNCTIONAL: Log connection establishment
     * Cognitive Complexity: 2
     */
    private WebSocketConnection logConnectionEstablished(WebSocketConnection connection) {
        String userId = java.util.Optional.ofNullable(connection.auth())
            .map(auth -> auth.userDetails())
            .map(details -> details.userId())
            .orElse("unknown");
        log.info("WebSocket connection registered: session={}, user={}", 
            connection.sessionId(), userId);
        return connection;
    }
    
    /**
     * ✅ FUNCTIONAL: Check if connection is healthy
     * Cognitive Complexity: 2
     */
    private boolean isConnectionHealthy(WebSocketConnection connection) {
        return connection.session().isOpen() && 
               connection.status() == ConnectionStatus.ACTIVE;
    }
    
    /**
     * ✅ FUNCTIONAL: Get connection statistics
     * Cognitive Complexity: 3
     */
    public Map<String, Object> getConnectionStatistics() {
        int totalConnections = activeConnections.size();
        long activeCount = activeConnections.values().stream()
            .filter(conn -> conn.status() == ConnectionStatus.ACTIVE)
            .count();
        long idleCount = activeConnections.values().stream()
            .filter(conn -> conn.status() == ConnectionStatus.IDLE)
            .count();
        
        return Map.of(
            "totalConnections", totalConnections,
            "activeConnections", activeCount,
            "idleConnections", idleCount,
            "disconnectingConnections", totalConnections - activeCount - idleCount,
            "lastUpdated", Instant.now().toString()
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Force disconnect session
     * Cognitive Complexity: 4
     */
    public Result<String, GatewayError> forceDisconnectSession(String sessionId) {
        return java.util.Optional.ofNullable(activeConnections.get(sessionId))
            .map(connection -> {
                try {
                    connection.session().close();
                    activeConnections.remove(sessionId);
                    log.info("Forced disconnect for session: {}", sessionId);
                    return Result.<String, GatewayError>success("Session " + sessionId + " disconnected");
                } catch (Exception e) {
                    log.error("Failed to force disconnect session: {}", sessionId, e);
                    return Result.<String, GatewayError>failure(
                        new GatewayError.SystemError.InternalServerError(
                            "Failed to disconnect session: " + e.getMessage(),
                            "disconnect-" + System.currentTimeMillis()));
                }
            })
            .orElse(Result.failure(
                new GatewayError.ValidationError.InvalidInput(
                    "Session not found: " + sessionId,
                    "sessionId",
                    sessionId)));
    }
    
    // ✅ IMMUTABLE: WebSocket connection record
    public record WebSocketConnection(
        String sessionId,
        WebSocketSession session,
        SecurityAuthenticationService.AuthenticationResult auth,
        Instant establishedTime,
        java.util.Optional<Instant> lastActivityTime,
        ConnectionStatus status
    ) {
        // Convenience methods for WebSocketEventRouter
        public Set<String> subscriptionTopics() {
            // Extract from auth result or session attributes
            return auth.userDetails().permissions().stream()
                .filter(p -> p.startsWith("SUBSCRIBE_"))
                .map(p -> p.replace("SUBSCRIBE_", ""))
                .collect(java.util.stream.Collectors.toSet());
        }
        
        public String userId() {
            return auth.userDetails().userId();
        }
        
        public Map<String, Object> metadata() {
            return Map.of(
                "roles", auth.userDetails().roles(),
                "permissions", auth.userDetails().permissions(),
                "sessionId", sessionId,
                "establishedTime", establishedTime
            );
        }
    }
    
    // ✅ IMMUTABLE: Connection status enum
    public enum ConnectionStatus {
        ACTIVE,
        IDLE,
        DISCONNECTING,
        TERMINATED
    }
}
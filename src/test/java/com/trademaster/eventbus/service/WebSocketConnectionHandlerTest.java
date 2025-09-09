package com.trademaster.eventbus.service;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import com.trademaster.eventbus.service.WebSocketConnectionHandler.WebSocketConnection;
import com.trademaster.eventbus.service.WebSocketConnectionHandler.ConnectionStatus;
import com.trademaster.eventbus.service.SecurityAuthenticationService.AuthenticationResult;
import com.trademaster.eventbus.service.SecurityAuthenticationService.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.fail;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * ✅ UNIT TESTS: WebSocketConnectionHandler Test Suite
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #20: >80% unit test coverage with functional test builders
 * - Virtual Threads testing with async CompletableFuture patterns
 * - Functional programming test patterns with Result types
 * - Property-based testing for connection lifecycle
 * 
 * TEST COVERAGE:
 * - Connection registration and lifecycle management
 * - Connection storage and retrieval operations
 * - Error handling with functional Result patterns
 * - Virtual Threads execution and concurrency
 * - Connection cleanup and termination
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per test class
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketConnectionHandlerTest {

    private WebSocketConnectionHandler connectionHandler;

    @Mock
    private WebSocketSession mockSession;
    
    @Mock
    private AuthenticationResult mockAuthResult;

    @BeforeEach
    void setUp() {
        connectionHandler = new WebSocketConnectionHandler();
    }

    /**
     * ✅ TEST: Successful connection registration
     */
    @Test
    void shouldRegisterConnectionSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Valid session and authentication result
        String sessionId = "test-session-123";
        String userId = "user-456";
        when(mockSession.getId()).thenReturn(sessionId);
        
        UserDetails userDetails = new UserDetails(userId, "test@example.com", "Test User", null, Map.of());
        when(mockAuthResult.userId()).thenReturn(userId);
        when(mockAuthResult.roles()).thenReturn(Set.of("TRADER", "VIEWER"));
        when(mockAuthResult.userDetails()).thenReturn(userDetails);
        when(mockAuthResult.authenticationTime()).thenReturn(Instant.now());

        // ✅ WHEN: Registering connection
        CompletableFuture<Result<WebSocketConnection, GatewayError>> future = 
            connectionHandler.registerConnection(mockSession, mockAuthResult);

        // ✅ THEN: Connection registered successfully
        Result<WebSocketConnection, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        WebSocketConnection connection = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals(sessionId, connection.sessionId());
        assertEquals(userId, connection.auth().userId());
        assertEquals(ConnectionStatus.ACTIVE, connection.status());
        assertNotNull(connection.establishedTime());
    }

    /**
     * ✅ TEST: Connection registration with null session
     */
    @Test
    void shouldFailConnectionRegistrationWithNullSession() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Null session
        WebSocketSession nullSession = null;

        // ✅ WHEN: Registering connection with null session
        CompletableFuture<Result<WebSocketConnection, GatewayError>> future = 
            connectionHandler.registerConnection(nullSession, mockAuthResult);

        // ✅ THEN: Registration fails with internal server error
        Result<WebSocketConnection, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.fold(
            errorValue -> errorValue,
            success -> fail("Expected failure but got success: " + success)
        );
        assertEquals(GatewayError.ErrorType.INTERNAL_ERROR, error.type());
        assertTrue(error.message().contains("Invalid"));
    }

    /**
     * ✅ TEST: Connection registration with null authentication
     */
    @Test
    void shouldFailConnectionRegistrationWithNullAuth() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Valid session but null authentication
        String sessionId = "test-session-123";
        when(mockSession.getId()).thenReturn(sessionId);
        AuthenticationResult nullAuth = null;

        // ✅ WHEN: Registering connection with null authentication
        CompletableFuture<Result<WebSocketConnection, GatewayError>> future = 
            connectionHandler.registerConnection(mockSession, nullAuth);

        // ✅ THEN: Registration fails with internal server error
        Result<WebSocketConnection, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.fold(
            errorValue -> errorValue,
            success -> fail("Expected failure but got success: " + success)
        );
        assertEquals(GatewayError.ErrorType.INTERNAL_ERROR, error.type());
        assertTrue(error.message().contains("Invalid"));
    }

    /**
     * ✅ TEST: Successful connection cleanup
     */
    @Test
    void shouldCleanupConnectionSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Register a connection first
        String sessionId = "test-session-123";
        String userId = "user-456";
        when(mockSession.getId()).thenReturn(sessionId);
        
        UserDetails userDetails = new UserDetails(userId, "test@example.com", "Test User", null, Map.of());
        when(mockAuthResult.userId()).thenReturn(userId);
        when(mockAuthResult.roles()).thenReturn(Set.of("TRADER"));
        when(mockAuthResult.userDetails()).thenReturn(userDetails);
        when(mockAuthResult.authenticationTime()).thenReturn(Instant.now());

        // Register connection
        CompletableFuture<Result<WebSocketConnection, GatewayError>> registerFuture = 
            connectionHandler.registerConnection(mockSession, mockAuthResult);
        Result<WebSocketConnection, GatewayError> registerResult = registerFuture.get();
        assertTrue(registerResult.isSuccess());

        // ✅ WHEN: Cleaning up connection
        CompletableFuture<Result<Void, GatewayError>> future = 
            connectionHandler.cleanupConnection(sessionId);

        // ✅ THEN: Connection cleaned up successfully
        Result<Void, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
    }

    /**
     * ✅ TEST: Cleanup non-existent connection
     */
    @Test
    void shouldHandleCleanupOfNonExistentConnection() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: No active connections
        String nonExistentSessionId = "non-existent-session";

        // ✅ WHEN: Attempting to cleanup non-existent connection
        CompletableFuture<Result<Void, GatewayError>> future = 
            connectionHandler.cleanupConnection(nonExistentSessionId);

        // ✅ THEN: Operation fails with error
        Result<Void, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.fold(
            errorValue -> errorValue,
            success -> fail("Expected failure but got success: " + success)
        );
        assertEquals(GatewayError.ErrorType.INTERNAL_ERROR, error.type());
    }

    /**
     * ✅ TEST: Get connection by session ID
     */
    @Test
    void shouldRetrieveConnectionBySessionId() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Register a connection first
        String sessionId = "test-session-123";
        String userId = "user-456";
        when(mockSession.getId()).thenReturn(sessionId);
        
        UserDetails userDetails = new UserDetails(userId, "test@example.com", "Test User", null, Map.of());
        when(mockAuthResult.userId()).thenReturn(userId);
        when(mockAuthResult.roles()).thenReturn(Set.of("TRADER"));
        when(mockAuthResult.userDetails()).thenReturn(userDetails);
        when(mockAuthResult.authenticationTime()).thenReturn(Instant.now());

        // Register connection
        CompletableFuture<Result<WebSocketConnection, GatewayError>> registerFuture = 
            connectionHandler.registerConnection(mockSession, mockAuthResult);
        Result<WebSocketConnection, GatewayError> registerResult = registerFuture.get();
        assertTrue(registerResult.isSuccess());

        // ✅ WHEN: Getting connection by session ID
        Result<WebSocketConnection, GatewayError> result = 
            connectionHandler.getConnection(sessionId);

        // ✅ THEN: Connection retrieved successfully
        assertTrue(result.isSuccess());
        WebSocketConnection retrievedConnection = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals(sessionId, retrievedConnection.sessionId());
        assertEquals(userId, retrievedConnection.auth().userId());
    }

    /**
     * ✅ TEST: Get non-existent connection
     */
    @Test
    void shouldFailToRetrieveNonExistentConnection() {
        // ✅ GIVEN: No active connections
        String nonExistentSessionId = "non-existent-session";

        // ✅ WHEN: Getting non-existent connection
        Result<WebSocketConnection, GatewayError> result = 
            connectionHandler.getConnection(nonExistentSessionId);

        // ✅ THEN: Operation fails with invalid session error
        assertTrue(result.isFailure());
        GatewayError error = result.fold(
            errorValue -> errorValue,
            success -> fail("Expected failure but got success: " + success)
        );
        assertEquals(GatewayError.ErrorType.VALIDATION_ERROR, error.type());
        assertTrue(error.message().contains("Session not found"));
    }

    /**
     * ✅ TEST: Get connections by user ID
     */
    @Test
    void shouldRetrieveConnectionsByUserId() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Multiple connections registered
        String userId = "user-456";
        
        // Register first connection
        WebSocketSession session1 = mock(WebSocketSession.class);
        AuthenticationResult auth1 = mock(AuthenticationResult.class);
        UserDetails userDetails1 = new UserDetails(userId, "test1@example.com", "Test User 1", null, Map.of());
        when(session1.getId()).thenReturn("session-1");
        when(auth1.userId()).thenReturn(userId);
        when(auth1.roles()).thenReturn(Set.of("TRADER"));
        when(auth1.userDetails()).thenReturn(userDetails1);
        when(auth1.authenticationTime()).thenReturn(Instant.now());
        
        // Register second connection for same user
        WebSocketSession session2 = mock(WebSocketSession.class);
        AuthenticationResult auth2 = mock(AuthenticationResult.class);
        UserDetails userDetails2 = new UserDetails(userId, "test2@example.com", "Test User 2", null, Map.of());
        when(session2.getId()).thenReturn("session-2");
        when(auth2.userId()).thenReturn(userId);
        when(auth2.roles()).thenReturn(Set.of("VIEWER"));
        when(auth2.userDetails()).thenReturn(userDetails2);
        when(auth2.authenticationTime()).thenReturn(Instant.now());
        
        connectionHandler.registerConnection(session1, auth1).get();
        connectionHandler.registerConnection(session2, auth2).get();

        // ✅ WHEN: Getting connections by user ID
        Set<WebSocketConnection> userConnections = 
            connectionHandler.getUserConnections(userId);

        // ✅ THEN: Matching user connections returned
        assertEquals(2, userConnections.size());
        assertTrue(userConnections.stream().allMatch(conn -> userId.equals(conn.auth().userId())));
    }

    /**
     * ✅ TEST: Get active connection count
     */
    @Test
    void shouldProvideActiveConnectionCount() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Initially no connections
        assertEquals(0, connectionHandler.getActiveConnectionCount());

        // ✅ WHEN: Registering connections
        String sessionId1 = "session-1";
        WebSocketSession session1 = mock(WebSocketSession.class);
        AuthenticationResult auth1 = mock(AuthenticationResult.class);
        UserDetails userDetails1 = new UserDetails("user-1", "test1@example.com", "Test User 1", null, Map.of());
        when(session1.getId()).thenReturn(sessionId1);
        when(auth1.userId()).thenReturn("user-1");
        when(auth1.roles()).thenReturn(Set.of("TRADER"));
        when(auth1.userDetails()).thenReturn(userDetails1);
        when(auth1.authenticationTime()).thenReturn(Instant.now());
        
        connectionHandler.registerConnection(session1, auth1).get();

        // ✅ THEN: Connection count updated
        assertEquals(1, connectionHandler.getActiveConnectionCount());
    }

    /**
     * ✅ TEST: Connection health check
     */
    @Test
    void shouldCheckConnectionHealth() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Register a connection
        String sessionId = "test-session-123";
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.isOpen()).thenReturn(true);
        
        UserDetails userDetails = new UserDetails("user-456", "test@example.com", "Test User", null, Map.of());
        when(mockAuthResult.userId()).thenReturn("user-456");
        when(mockAuthResult.roles()).thenReturn(Set.of("TRADER"));
        when(mockAuthResult.userDetails()).thenReturn(userDetails);
        when(mockAuthResult.authenticationTime()).thenReturn(Instant.now());

        connectionHandler.registerConnection(mockSession, mockAuthResult).get();

        // ✅ WHEN: Checking connection health
        CompletableFuture<Map<String, Boolean>> future = connectionHandler.checkConnectionHealth();

        // ✅ THEN: Health status returned
        Map<String, Boolean> healthStatus = future.get();
        assertNotNull(healthStatus);
        assertEquals(1, healthStatus.size());
        assertTrue(healthStatus.containsKey(sessionId));
    }

    /**
     * ✅ TEST: Concurrent connection registration
     */
    @Test
    void shouldHandleConcurrentConnectionRegistrations() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Multiple concurrent registration attempts
        int concurrentCount = 50;
        CompletableFuture<Result<WebSocketConnection, GatewayError>>[] futures = 
            new CompletableFuture[concurrentCount];

        // ✅ WHEN: Registering connections concurrently
        for (int i = 0; i < concurrentCount; i++) {
            WebSocketSession session = mock(WebSocketSession.class);
            AuthenticationResult auth = mock(AuthenticationResult.class);
            UserDetails userDetails = new UserDetails("user-" + i, "test" + i + "@example.com", "Test User " + i, null, Map.of());
            when(session.getId()).thenReturn("session-" + i);
            when(auth.userId()).thenReturn("user-" + i);
            when(auth.roles()).thenReturn(Set.of("TRADER"));
            when(auth.userDetails()).thenReturn(userDetails);
            when(auth.authenticationTime()).thenReturn(Instant.now());
            
            futures[i] = connectionHandler.registerConnection(session, auth);
        }

        // ✅ THEN: All registrations complete successfully
        CompletableFuture.allOf(futures).get();
        
        assertEquals(concurrentCount, connectionHandler.getActiveConnectionCount());
        for (CompletableFuture<Result<WebSocketConnection, GatewayError>> future : futures) {
            assertTrue(future.get().isSuccess());
        }
    }
}
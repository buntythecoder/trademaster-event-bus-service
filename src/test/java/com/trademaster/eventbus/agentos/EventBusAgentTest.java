package com.trademaster.eventbus.agentos;

import com.trademaster.eventbus.domain.Result;
import com.trademaster.eventbus.domain.GatewayError;
import com.trademaster.eventbus.service.PerformanceMonitoringService;
import com.trademaster.eventbus.service.WebSocketConnectionHandler;
import com.trademaster.eventbus.service.CircuitBreakerService;
import com.trademaster.eventbus.agentos.EventBusAgent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ✅ UNIT TESTS: EventBusAgent Test Suite
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventBusAgentTest {

    @Mock
    private PerformanceMonitoringService performanceService;

    @Mock
    private WebSocketConnectionHandler connectionHandler;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    @InjectMocks
    private EventBusAgent agent;

    @BeforeEach
    void setUp() {
        // Setup mock behaviors
        when(performanceService.getSlaComplianceRate()).thenReturn(96.5);
        when(performanceService.getPerformanceStatistics()).thenReturn(
            new PerformanceMonitoringService.PerformanceStatistics(
                5000L, 175L, 96.5, 15, Instant.now()
            )
        );
        when(connectionHandler.getConnectionStatistics()).thenReturn(
            Map.of("activeConnections", 250, "totalConnections", 1000)
        );
        when(circuitBreakerService.getDatabaseCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getMessageQueueCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getExternalServiceCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getWebSocketCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getEventProcessingCircuitBreakerStatus()).thenReturn("CLOSED");
    }

    @Test
    void shouldInitializeAgentSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Initializing agent
        CompletableFuture<Result<AgentRegistration, GatewayError>> future = agent.initializeAgent();

        // ✅ THEN: Agent initialized successfully
        Result<AgentRegistration, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        AgentRegistration registration = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertNotNull(registration.agentId());
        assertEquals("Event Bus Agent", registration.name());
        assertEquals("1.0.0", registration.version());
        assertEquals(AgentType.EVENT_PROCESSING, registration.type());
        assertEquals(4, registration.capabilities().size()); // 4 capabilities registered
        
        // Verify capabilities
        assertTrue(registration.capabilities().stream()
            .anyMatch(cap -> cap.id().equals("websocket-management")));
        assertTrue(registration.capabilities().stream()
            .anyMatch(cap -> cap.id().equals("event-processing")));
        assertTrue(registration.capabilities().stream()
            .anyMatch(cap -> cap.id().equals("circuit-breaker-management")));
        assertTrue(registration.capabilities().stream()
            .anyMatch(cap -> cap.id().equals("performance-monitoring")));
    }

    @Test
    void shouldGetAgentHealthSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized
        agent.initializeAgent().get();

        // ✅ WHEN: Getting agent health
        CompletableFuture<Result<AgentHealth, GatewayError>> future = agent.getAgentHealth();

        // ✅ THEN: Agent health retrieved successfully
        Result<AgentHealth, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        AgentHealth health = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertNotNull(health.agentId());
        assertEquals(AgentHealthStatus.HEALTHY, health.status()); // 96.5% > 95%
        assertEquals(96.5, health.slaCompliance());
        
        Map<String, Object> connectionStats = health.connectionStats();
        assertEquals(250, connectionStats.get("activeConnections"));
        assertEquals(1000, connectionStats.get("totalConnections"));
        
        assertTrue(health.systemMetrics().containsKey("circuitBreakerStatus"));
        assertTrue(health.systemMetrics().containsKey("lastHealthCheck"));
    }

    @Test
    void shouldHandleGetCapabilitiesMCPMessage() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized and MCP message for capabilities
        agent.initializeAgent().get();
        
        MCPMessage message = new MCPMessage(
            "test-id-1",
            "GET_CAPABILITIES",
            Map.of(),
            Instant.now()
        );

        // ✅ WHEN: Handling MCP message
        CompletableFuture<Result<MCPResponse, GatewayError>> future = agent.handleMCPMessage(message);

        // ✅ THEN: MCP message handled successfully
        Result<MCPResponse, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        MCPResponse response = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals("test-id-1", response.id());
        assertTrue(response.success());
        assertTrue(response.data().containsKey("capabilities"));
    }

    @Test
    void shouldHandleGetHealthMCPMessage() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized and MCP message for health
        agent.initializeAgent().get();
        
        MCPMessage message = new MCPMessage(
            "test-id-2",
            "GET_HEALTH",
            Map.of(),
            Instant.now()
        );

        // ✅ WHEN: Handling MCP message
        CompletableFuture<Result<MCPResponse, GatewayError>> future = agent.handleMCPMessage(message);

        // ✅ THEN: MCP message handled successfully
        Result<MCPResponse, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        MCPResponse response = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals("test-id-2", response.id());
        assertTrue(response.success());
        assertTrue(response.data().containsKey("health"));
    }

    @Test
    void shouldHandleGetPerformanceMCPMessage() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized and MCP message for performance
        agent.initializeAgent().get();
        
        MCPMessage message = new MCPMessage(
            "test-id-3",
            "GET_PERFORMANCE",
            Map.of(),
            Instant.now()
        );

        // ✅ WHEN: Handling MCP message
        CompletableFuture<Result<MCPResponse, GatewayError>> future = agent.handleMCPMessage(message);

        // ✅ THEN: MCP message handled successfully
        Result<MCPResponse, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        MCPResponse response = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals("test-id-3", response.id());
        assertTrue(response.success());
        assertTrue(response.data().containsKey("performance"));
    }

    @Test
    void shouldHandleUnknownMCPMessageType() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized and unknown MCP message
        agent.initializeAgent().get();
        
        MCPMessage message = new MCPMessage(
            "test-id-unknown",
            "UNKNOWN_MESSAGE_TYPE",
            Map.of(),
            Instant.now()
        );

        // ✅ WHEN: Handling unknown MCP message
        CompletableFuture<Result<MCPResponse, GatewayError>> future = agent.handleMCPMessage(message);

        // ✅ THEN: MCP message handled with error response
        Result<MCPResponse, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        MCPResponse response = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals("test-id-unknown", response.id());
        assertFalse(response.success());
        assertEquals("UNKNOWN_MESSAGE_TYPE", response.errorCode());
        assertTrue(response.errorMessage().contains("Message type not supported"));
    }

    @Test
    void shouldExecuteMaintenanceCommandsSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized
        agent.initializeAgent().get();

        // ✅ WHEN: Executing start maintenance command
        AgentCommand startCommand = new AgentCommand(
            "cmd-1",
            "START_MAINTENANCE",
            Map.of(),
            Instant.now()
        );
        
        CompletableFuture<Result<String, GatewayError>> future1 = agent.executeCommand(startCommand);
        Result<String, GatewayError> result1 = future1.get();
        
        // ✅ THEN: Maintenance started successfully
        assertTrue(result1.isSuccess());
        assertEquals("Agent placed in maintenance mode", result1.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        ));
        
        // ✅ WHEN: Executing end maintenance command
        AgentCommand endCommand = new AgentCommand(
            "cmd-2",
            "END_MAINTENANCE",
            Map.of(),
            Instant.now()
        );
        
        CompletableFuture<Result<String, GatewayError>> future2 = agent.executeCommand(endCommand);
        Result<String, GatewayError> result2 = future2.get();
        
        // ✅ THEN: Maintenance ended successfully
        assertTrue(result2.isSuccess());
        assertEquals("Agent returned to active mode", result2.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        ));
    }

    @Test
    void shouldExecuteRefreshCapabilitiesCommand() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized
        agent.initializeAgent().get();

        // ✅ WHEN: Executing refresh capabilities command
        AgentCommand command = new AgentCommand(
            "cmd-refresh",
            "REFRESH_CAPABILITIES",
            Map.of(),
            Instant.now()
        );
        
        CompletableFuture<Result<String, GatewayError>> future = agent.executeCommand(command);

        // ✅ THEN: Capabilities refreshed successfully
        Result<String, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        assertEquals("Capabilities refreshed successfully", result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        ));
    }

    @Test
    void shouldDetectDegradedHealthWhenSlaLow() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Agent is initialized with low SLA compliance
        when(performanceService.getSlaComplianceRate()).thenReturn(90.0); // Below 95%
        agent.initializeAgent().get();

        // ✅ WHEN: Getting agent health
        CompletableFuture<Result<AgentHealth, GatewayError>> future = agent.getAgentHealth();

        // ✅ THEN: Agent health shows degraded status
        Result<AgentHealth, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        AgentHealth health = result.fold(
            error -> fail("Expected success but got error: " + error.message()),
            success -> success
        );
        assertEquals(AgentHealthStatus.DEGRADED, health.status());
        assertEquals(90.0, health.slaCompliance());
    }
}
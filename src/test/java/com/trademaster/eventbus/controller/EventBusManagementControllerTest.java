package com.trademaster.eventbus.controller;

import com.trademaster.eventbus.service.PerformanceMonitoringService;
import com.trademaster.eventbus.service.WebSocketConnectionHandler;
import com.trademaster.eventbus.service.CircuitBreakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ✅ UNIT TESTS: EventBusManagementController Test Suite
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventBusManagementControllerTest {

    @Mock
    private PerformanceMonitoringService performanceService;

    @Mock
    private WebSocketConnectionHandler connectionHandler;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    @InjectMocks
    private EventBusManagementController controller;

    @BeforeEach
    void setUp() {
        // Setup mock behaviors
        when(performanceService.getSlaComplianceRate()).thenReturn(95.5);
        when(performanceService.getPerformanceStatistics()).thenReturn(
            new PerformanceMonitoringService.PerformanceStatistics(
                1000L, 25L, 97.5, 10, java.time.Instant.now()
            )
        );
        when(connectionHandler.getConnectionStatistics()).thenReturn(
            Map.of("activeConnections", 150, "totalConnections", 500)
        );
        when(circuitBreakerService.getDatabaseCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getMessageQueueCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getExternalServiceCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getWebSocketCircuitBreakerStatus()).thenReturn("CLOSED");
        when(circuitBreakerService.getEventProcessingCircuitBreakerStatus()).thenReturn("CLOSED");
    }

    @Test
    void shouldGetPerformanceStatisticsSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting performance statistics
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getPerformanceStatistics();

        // ✅ THEN: Statistics retrieved successfully
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1000L, body.get("totalOperations"));
        assertEquals(25L, body.get("slaViolations"));
        assertEquals(95.5, body.get("complianceRate"));
        assertEquals("HEALTHY", body.get("status"));
    }

    @Test
    void shouldGetConnectionStatisticsSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting connection statistics
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getConnectionStatistics();

        // ✅ THEN: Connection statistics retrieved successfully
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(150, body.get("activeConnections"));
        assertEquals(500, body.get("totalConnections"));
    }

    @Test
    void shouldGetCircuitBreakerStatusSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting circuit breaker status
        CompletableFuture<ResponseEntity<Map<String, String>>> future = controller.getCircuitBreakerStatus();

        // ✅ THEN: Circuit breaker status retrieved successfully
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("CLOSED", body.get("database"));
        assertEquals("CLOSED", body.get("messageQueue"));
        assertEquals("CLOSED", body.get("externalService"));
        assertEquals("CLOSED", body.get("webSocket"));
        assertEquals("CLOSED", body.get("eventProcessing"));
    }

    @Test
    void shouldForceCircuitBreakerStateSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Circuit breaker force operation
        doNothing().when(circuitBreakerService).forceCircuitBreakerOpen("database");

        // ✅ WHEN: Forcing circuit breaker to open
        CompletableFuture<ResponseEntity<Map<String, String>>> future = 
            controller.forceCircuitBreakerState("database", "OPEN");

        // ✅ THEN: Circuit breaker state changed successfully
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("database", body.get("circuitBreaker"));
        assertEquals("OPEN", body.get("requestedState"));
        assertEquals("SUCCESS", body.get("result"));
        
        verify(circuitBreakerService).forceCircuitBreakerOpen("database");
    }

    @Test
    void shouldGetSlaComplianceReportSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting SLA compliance report
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getSlaComplianceReport();

        // ✅ THEN: SLA compliance report retrieved successfully
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(95.5, body.get("complianceRate"));
        assertEquals(1000L, body.get("totalOperations"));
        assertEquals(25L, body.get("violations"));
        assertEquals("COMPLIANT", body.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> thresholds = (Map<String, String>) body.get("thresholds");
        assertEquals("25ms", thresholds.get("critical"));
        assertEquals("50ms", thresholds.get("high"));
        assertEquals("100ms", thresholds.get("standard"));
        assertEquals("500ms", thresholds.get("background"));
    }

    @Test
    void shouldHandleInvalidCircuitBreakerState() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Using invalid circuit breaker state
        CompletableFuture<ResponseEntity<Map<String, String>>> future = 
            controller.forceCircuitBreakerState("database", "INVALID_STATE");

        // ✅ THEN: Invalid state handled gracefully
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_STATE", body.get("result"));
    }
}
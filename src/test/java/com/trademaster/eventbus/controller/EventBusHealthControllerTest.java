package com.trademaster.eventbus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ✅ UNIT TESTS: EventBusHealthController Test Suite
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventBusHealthControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventBusHealthController controller;

    @BeforeEach
    void setUp() {
        // Setup healthy responses by default
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
    }

    @Test
    void shouldReturnHealthyStatusWhenAllServicesUp() {
        // ✅ WHEN: Getting overall health
        Health health = controller.health();

        // ✅ THEN: Service is healthy
        assertEquals(Health.up().build().getStatus(), health.getStatus());
        assertNotNull(health.getDetails().get("timestamp"));
        assertEquals("All systems operational", health.getDetails().get("status"));
    }

    @Test
    void shouldGetDetailedHealthSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting detailed health
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getDetailedHealth();

        // ✅ THEN: Detailed health retrieved successfully
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("database"));
        assertTrue(body.containsKey("redis"));
        assertTrue(body.containsKey("kafka"));
        assertTrue(body.containsKey("websocket"));
        assertTrue(body.containsKey("circuitBreakers"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("overallStatus"));
    }

    @Test
    void shouldGetDatabaseHealthSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting database health
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getDatabaseHealth();

        // ✅ THEN: Database health retrieved successfully
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("timestamp"));
        assertEquals("Database connection successful", body.get("details"));
        
        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }

    @Test
    void shouldHandleDatabaseConnectionFailure() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Database connection fails
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new RuntimeException("Connection failed"));

        // ✅ WHEN: Getting database health
        CompletableFuture<ResponseEntity<Map<String, Object>>> future = controller.getDatabaseHealth();

        // ✅ THEN: Database health shows DOWN status
        ResponseEntity<Map<String, Object>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
        assertEquals("Database connection failed", body.get("details"));
    }

    @Test
    void shouldGetReadinessProbeSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting readiness probe
        CompletableFuture<ResponseEntity<Map<String, String>>> future = controller.getReadinessProbe();

        // ✅ THEN: Readiness probe returns UP
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("timestamp"));
    }

    @Test
    void shouldGetLivenessProbeSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ WHEN: Getting liveness probe
        CompletableFuture<ResponseEntity<Map<String, String>>> future = controller.getLivenessProbe();

        // ✅ THEN: Liveness probe always returns UP
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(200, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("timestamp"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenSystemsDown() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Database is down
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new RuntimeException("Database down"));

        // ✅ WHEN: Getting readiness probe
        CompletableFuture<ResponseEntity<Map<String, String>>> future = controller.getReadinessProbe();

        // ✅ THEN: Service returns 503 (Service Unavailable)
        ResponseEntity<Map<String, String>> response = future.get();
        assertEquals(503, response.getStatusCodeValue());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
    }
}
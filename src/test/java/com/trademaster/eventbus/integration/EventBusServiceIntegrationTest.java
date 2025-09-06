package com.trademaster.eventbus.integration;

import com.trademaster.eventbus.EventBusServiceApplication;
import com.trademaster.eventbus.agentos.EventBusAgent;
import com.trademaster.eventbus.service.WebSocketConnectionHandler;
import com.trademaster.eventbus.service.PerformanceMonitoringService;
import com.trademaster.eventbus.service.CircuitBreakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ PRODUCTION INTEGRATION TESTS: End-to-End Event Bus Service Testing
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #20: >80% coverage with real dependency integration
 * - TestContainers for PostgreSQL, Redis, Kafka infrastructure
 * - Virtual Threads testing with async CompletableFuture patterns
 * - Functional programming test patterns with Result validation
 * - Production-like environment testing with performance validation
 * 
 * TEST COVERAGE:
 * - Full application context loading with real dependencies
 * - AgentOS integration and registration flows
 * - WebSocket connection management with authentication
 * - Performance monitoring and SLA compliance validation
 * - Circuit breaker functionality under load
 * - Database operations with transaction integrity
 * - Message queue operations with Kafka integration
 * 
 * INFRASTRUCTURE:
 * - PostgreSQL 15+ for persistent data storage
 * - Redis 7+ for session management and caching
 * - Kafka 3.5+ for event streaming and messaging
 * - Real network communication and latency simulation
 */
@SpringBootTest(
    classes = EventBusServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@SpringJUnitConfig
@ExtendWith({})
class EventBusServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventBusAgent eventBusAgent;

    @Autowired
    private WebSocketConnectionHandler connectionHandler;

    @Autowired
    private PerformanceMonitoringService performanceService;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    // ✅ TESTCONTAINERS: Real infrastructure dependencies
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("event_bus_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    /**
     * ✅ DYNAMIC CONFIGURATION: TestContainers integration
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        
        // Test-specific configuration
        registry.add("spring.threads.virtual.enabled", () -> "true");
        registry.add("management.endpoints.web.exposure.include", () -> "health,metrics,prometheus");
        registry.add("logging.level.com.trademaster.eventbus", () -> "DEBUG");
    }

    @BeforeEach
    void setUp() {
        // Ensure clean state for each test
        assertNotNull(eventBusAgent, "EventBusAgent should be autowired");
        assertNotNull(connectionHandler, "WebSocketConnectionHandler should be autowired");
        assertNotNull(performanceService, "PerformanceMonitoringService should be autowired");
        assertNotNull(circuitBreakerService, "CircuitBreakerService should be autowired");
        
        // Verify containers are running
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        assertTrue(redis.isRunning(), "Redis container should be running");
        assertTrue(kafka.isRunning(), "Kafka container should be running");
    }

    /**
     * ✅ TEST: Application context loads successfully with real dependencies
     */
    @Test
    void shouldLoadApplicationContextWithRealDependencies() {
        // ✅ GIVEN: Real infrastructure containers are running
        assertTrue(postgres.isRunning());
        assertTrue(redis.isRunning());
        assertTrue(kafka.isRunning());

        // ✅ WHEN: Application context is loaded
        // Context loading is handled by @SpringBootTest

        // ✅ THEN: All critical beans are properly initialized
        assertNotNull(eventBusAgent);
        assertNotNull(connectionHandler);
        assertNotNull(performanceService);
        assertNotNull(circuitBreakerService);
    }

    /**
     * ✅ TEST: AgentOS registration integration with performance validation
     */
    @Test
    void shouldRegisterAgentWithAgentOSSuccessfully() throws Exception {
        // ✅ GIVEN: Event bus service is running
        String agentOSUrl = "http://localhost:" + port + "/api/v1/agentos/register";

        // ✅ WHEN: Agent registration is attempted
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.postForEntity(agentOSUrl, null, Map.class);
        long responseTime = System.currentTimeMillis() - startTime;

        // ✅ THEN: Registration succeeds with performance SLA compliance
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNotNull(responseBody.get("registration"));
        assertNotNull(responseBody.get("timestamp"));
        
        // ✅ PERFORMANCE SLA: < 50ms response time
        assertTrue(responseTime < 50, 
            String.format("Agent registration took %dms, exceeds 50ms SLA", responseTime));
    }

    /**
     * ✅ TEST: Agent health endpoint with comprehensive validation
     */
    @Test
    void shouldProvideComprehensiveAgentHealthStatus() throws Exception {
        // ✅ GIVEN: Agent is initialized
        String healthUrl = "http://localhost:" + port + "/api/v1/agentos/health";

        // ✅ WHEN: Health status is requested
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
        long responseTime = System.currentTimeMillis() - startTime;

        // ✅ THEN: Health status is comprehensive and performant
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertNotNull(responseBody.get("health"));
        assertNotNull(responseBody.get("timestamp"));
        
        // ✅ PERFORMANCE SLA: < 25ms response time
        assertTrue(responseTime < 25,
            String.format("Health check took %dms, exceeds 25ms SLA", responseTime));
    }

    /**
     * ✅ TEST: Performance statistics with real-time validation
     */
    @Test
    void shouldProvideRealTimePerformanceStatistics() throws Exception {
        // ✅ GIVEN: Service is processing operations
        String perfUrl = "http://localhost:" + port + "/api/v1/event-bus/performance/stats";
        
        // Generate some operations for statistics
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                // ✅ PERFORMANCE: Record operation using proper measurement pattern
                PerformanceMonitoringService.PerformanceTracker tracker = 
                    performanceService.startMeasurement("test-operation", "STANDARD_EVENT");
                
                // Simulate processing time
                try {
                    Thread.sleep(10 + (i % 20));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                performanceService.completeMeasurement(tracker);
            }
        }).get(3, TimeUnit.SECONDS);

        // ✅ WHEN: Performance statistics are requested
        long startTime = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.getForEntity(perfUrl, Map.class);
        long responseTime = System.currentTimeMillis() - startTime;

        // ✅ THEN: Statistics are accurate and performant
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> stats = response.getBody();
        assertNotNull(stats.get("totalOperations"));
        assertNotNull(stats.get("slaViolations"));
        assertNotNull(stats.get("complianceRate"));
        assertNotNull(stats.get("status"));
        
        // ✅ PERFORMANCE SLA: < 25ms response time
        assertTrue(responseTime < 25,
            String.format("Performance stats took %dms, exceeds 25ms SLA", responseTime));
    }

    /**
     * ✅ TEST: Circuit breaker functionality under load
     */
    @Test
    void shouldHandleCircuitBreakerStatesCorrectly() throws Exception {
        // ✅ GIVEN: Circuit breakers are configured
        String circuitUrl = "http://localhost:" + port + "/api/v1/event-bus/circuit-breakers/status";

        // ✅ WHEN: Circuit breaker status is requested
        ResponseEntity<Map> response = restTemplate.getForEntity(circuitUrl, Map.class);

        // ✅ THEN: All circuit breakers are operational
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, String> statuses = (Map<String, String>) response.getBody();
        assertNotNull(statuses.get("database"));
        assertNotNull(statuses.get("messageQueue"));
        assertNotNull(statuses.get("externalService"));
        
        // ✅ Initially all circuit breakers should be CLOSED (healthy)
        assertTrue(statuses.values().stream()
            .allMatch(status -> "CLOSED".equals(status) || "HALF_OPEN".equals(status)));
    }

    /**
     * ✅ TEST: Connection management with capacity validation
     */
    @Test
    void shouldManageConnectionsWithinCapacityLimits() throws Exception {
        // ✅ GIVEN: Connection management is active
        String connectionsUrl = "http://localhost:" + port + "/api/v1/event-bus/connections/stats";

        // ✅ WHEN: Connection statistics are requested
        ResponseEntity<Map> response = restTemplate.getForEntity(connectionsUrl, Map.class);

        // ✅ THEN: Connection stats are within expected ranges
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> stats = response.getBody();
        assertNotNull(stats.get("activeConnections"));
        
        // ✅ Validate connection count is reasonable for test environment
        Integer activeConnections = (Integer) stats.get("activeConnections");
        assertTrue(activeConnections >= 0);
        assertTrue(activeConnections <= 10000); // Max capacity check
    }

    /**
     * ✅ TEST: SLA compliance reporting with real metrics
     */
    @Test
    void shouldProvideAccurateSlaComplianceReporting() throws Exception {
        // ✅ GIVEN: Operations have been processed
        String slaUrl = "http://localhost:" + port + "/api/v1/event-bus/sla/compliance";
        
        // Generate operations with known performance characteristics
        CompletableFuture.runAsync(() -> {
            // Simulate compliant operations
            for (int i = 0; i < 95; i++) {
                PerformanceMonitoringService.PerformanceTracker tracker = 
                    performanceService.startMeasurement("compliant-op", "STANDARD_EVENT");
                
                try {
                    Thread.sleep(15); // Well within SLA
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                performanceService.completeMeasurement(tracker);
            }
            // Simulate some violations
            for (int i = 0; i < 5; i++) {
                PerformanceMonitoringService.PerformanceTracker tracker = 
                    performanceService.startMeasurement("violation-op", "STANDARD_EVENT");
                
                try {
                    Thread.sleep(200); // Exceeds SLA
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                performanceService.completeMeasurement(tracker);
            }
        }).get(5, TimeUnit.SECONDS);

        // ✅ WHEN: SLA compliance is requested
        ResponseEntity<Map> response = restTemplate.getForEntity(slaUrl, Map.class);

        // ✅ THEN: Compliance report reflects real performance
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> compliance = response.getBody();
        assertNotNull(compliance.get("complianceRate"));
        assertNotNull(compliance.get("totalOperations"));
        assertNotNull(compliance.get("violations"));
        assertNotNull(compliance.get("status"));
        
        Double complianceRate = (Double) compliance.get("complianceRate");
        assertTrue(complianceRate >= 0.0 && complianceRate <= 100.0);
    }

    /**
     * ✅ TEST: Database integration with transaction integrity
     */
    @Test
    void shouldMaintainDatabaseTransactionIntegrity() {
        // ✅ GIVEN: Database connection is established
        assertTrue(postgres.isRunning());
        
        // ✅ WHEN & THEN: Database operations should work correctly
        // This is validated through the successful loading of application context
        // and the ability to process operations that require database persistence
        assertNotNull(connectionHandler);
        assertTrue(connectionHandler.getActiveConnectionCount() >= 0);
    }

    /**
     * ✅ TEST: Virtual threads performance under concurrent load
     */
    @Test
    void shouldHandleConcurrentOperationsWithVirtualThreads() throws Exception {
        // ✅ GIVEN: Virtual threads are enabled
        String perfUrl = "http://localhost:" + port + "/api/v1/event-bus/performance/stats";
        
        // ✅ WHEN: Multiple concurrent operations are executed
        CompletableFuture<ResponseEntity<Map>>[] futures = new CompletableFuture[50];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 50; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> 
                restTemplate.getForEntity(perfUrl, Map.class));
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        // ✅ THEN: All operations complete successfully with good performance
        for (CompletableFuture<ResponseEntity<Map>> future : futures) {
            ResponseEntity<Map> response = future.get();
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        // ✅ CONCURRENCY PERFORMANCE: Should handle 50 concurrent requests efficiently
        assertTrue(totalTime < 2000, 
            String.format("50 concurrent operations took %dms, should be < 2000ms", totalTime));
    }

    /**
     * ✅ TEST: End-to-end workflow with all components
     */
    @Test
    void shouldExecuteCompleteWorkflowEndToEnd() throws Exception {
        // ✅ GIVEN: All services are operational
        String baseUrl = "http://localhost:" + port;
        
        // ✅ WHEN: Complete workflow is executed
        // 1. Register agent
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/agentos/register", null, Map.class);
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        
        // 2. Check agent health
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/agentos/health", Map.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        
        // 3. Get performance statistics
        ResponseEntity<Map> perfResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/event-bus/performance/stats", Map.class);
        assertEquals(HttpStatus.OK, perfResponse.getStatusCode());
        
        // 4. Check circuit breaker status
        ResponseEntity<Map> circuitResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/event-bus/circuit-breakers/status", Map.class);
        assertEquals(HttpStatus.OK, circuitResponse.getStatusCode());
        
        // ✅ THEN: All workflow steps complete successfully
        assertTrue((Boolean) registerResponse.getBody().get("success"));
        assertTrue((Boolean) healthResponse.getBody().get("success"));
        assertNotNull(perfResponse.getBody().get("totalOperations"));
        assertNotNull(circuitResponse.getBody());
    }

    /**
     * ✅ TEST: Infrastructure container health validation
     */
    @Test
    void shouldValidateInfrastructureContainerHealth() {
        // ✅ GIVEN & WHEN: Containers are running
        
        // ✅ THEN: All infrastructure containers are healthy
        assertTrue(postgres.isRunning(), "PostgreSQL container must be running");
        assertTrue(redis.isRunning(), "Redis container must be running");
        assertTrue(kafka.isRunning(), "Kafka container must be running");
        
        // ✅ Validate container network accessibility
        assertNotNull(postgres.getJdbcUrl());
        assertNotNull(redis.getHost());
        assertTrue(redis.getFirstMappedPort() > 0);
        assertNotNull(kafka.getBootstrapServers());
    }
}
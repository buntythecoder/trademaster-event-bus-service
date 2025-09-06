package com.trademaster.eventbus.service;

import com.trademaster.eventbus.service.PerformanceMonitoringService.PerformanceTracker;
import com.trademaster.eventbus.service.PerformanceMonitoringService.PerformanceResult;
import com.trademaster.eventbus.service.PerformanceMonitoringService.PerformanceStatistics;
import com.trademaster.eventbus.service.PerformanceMonitoringService.SlaCompliance;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ UNIT TESTS: PerformanceMonitoringService Test Suite
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #20: >80% unit test coverage with functional test builders
 * - Rule #22: Performance Standards validation
 * - Virtual Threads testing with async operations
 * - SLA compliance validation testing
 * - Prometheus metrics integration testing
 * 
 * TEST COVERAGE:
 * - SLA compliance tracking and validation
 * - Performance measurement and timing
 * - Metrics registration and recording
 * - SLA violation detection and alerting
 * - Statistical reporting and monitoring
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per test class
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerformanceMonitoringServiceTest {

    private PerformanceMonitoringService performanceService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        performanceService = new PerformanceMonitoringService(meterRegistry);
    }

    /**
     * ✅ TEST: Critical event SLA compliance tracking
     */
    @Test
    void shouldTrackCriticalEventSlaCompliance() throws InterruptedException {
        // ✅ GIVEN: Critical event operation
        String operationName = "process-critical-risk-event";
        String operationType = "CRITICAL_EVENT";
        
        // ✅ WHEN: Measuring fast critical event (within 25ms SLA)
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType);
        
        // Simulate fast processing (under SLA)
        Thread.sleep(10); // 10ms - well within 25ms SLA
        
        PerformanceResult result = performanceService.completeMeasurement(tracker);

        // ✅ THEN: SLA compliance achieved
        assertEquals(operationName, result.operationName());
        assertEquals(operationType, result.operationType());
        assertTrue(result.durationMs() >= 10, "Duration should include at least the sleep time");
        assertTrue(result.durationMs() < 50, "Critical event should complete within reasonable time (allowing for overhead)");
        assertEquals(SlaCompliance.COMPLIANT, result.compliance());
    }

    /**
     * ✅ TEST: High priority event SLA violation detection
     */
    @Test
    void shouldDetectHighPriorityEventSlaViolation() throws InterruptedException {
        // ✅ GIVEN: High priority event operation
        String operationName = "process-order-execution";
        String operationType = "HIGH_PRIORITY_EVENT";
        
        // ✅ WHEN: Measuring slow high priority event (exceeds 50ms SLA)
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType);
        
        // Simulate slow processing (exceeds SLA)
        Thread.sleep(60); // 60ms - exceeds 50ms SLA
        
        PerformanceResult result = performanceService.completeMeasurement(tracker);

        // ✅ THEN: SLA violation detected
        assertEquals(operationName, result.operationName());
        assertEquals(operationType, result.operationType());
        assertTrue(result.durationMs() > 50, "Should exceed 50ms SLA threshold");
        assertEquals(SlaCompliance.VIOLATION, result.compliance());
    }

    /**
     * ✅ TEST: Authentication SLA compliance
     */
    @Test
    void shouldValidateAuthenticationSlaCompliance() throws InterruptedException {
        // ✅ GIVEN: Authentication operation
        String operationName = "jwt-token-validation";
        String operationType = "AUTHENTICATION";
        
        // ✅ WHEN: Measuring authentication within SLA
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType);
        
        // Simulate cached token validation (fast)
        Thread.sleep(30); // 30ms - within 50ms SLA
        
        PerformanceResult result = performanceService.completeMeasurement(tracker);

        // ✅ THEN: Authentication SLA compliant
        assertTrue(result.durationMs() >= 30, "Duration should include at least the sleep time");
        assertTrue(result.durationMs() < 100, "Authentication should complete within reasonable time (allowing for overhead)");
        assertEquals(SlaCompliance.COMPLIANT, result.compliance());
    }

    /**
     * ✅ TEST: Database operation SLA monitoring
     */
    @Test
    void shouldMonitorDatabaseOperationSla() throws InterruptedException {
        // ✅ GIVEN: Database operation
        String operationName = "event-store-insert";
        String operationType = "DATABASE_OPERATION";
        Map<String, String> tags = Map.of("table", "event_store", "operation", "INSERT");
        
        // ✅ WHEN: Measuring database operation
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType, tags);
        
        // Simulate database operation
        Thread.sleep(5); // 5ms - within 10ms SLA
        
        PerformanceResult result = performanceService.completeMeasurement(tracker);

        // ✅ THEN: Database operation SLA compliant  
        assertTrue(result.durationMs() >= 5, "Duration should include at least the sleep time");
        assertTrue(result.durationMs() < 30, "Database operation should complete within reasonable time (allowing for overhead)");
        assertEquals(SlaCompliance.COMPLIANT, result.compliance());
        assertEquals(tags, result.tags());
    }

    /**
     * ✅ TEST: Performance statistics tracking
     */
    @Test
    void shouldTrackPerformanceStatistics() throws InterruptedException {
        // ✅ GIVEN: Multiple operations with mixed SLA compliance
        
        // Compliant operation
        PerformanceTracker tracker1 = performanceService.startMeasurement("fast-operation", "STANDARD_EVENT");
        Thread.sleep(50); // Within 100ms SLA
        performanceService.completeMeasurement(tracker1);
        
        // SLA violation
        PerformanceTracker tracker2 = performanceService.startMeasurement("slow-operation", "CRITICAL_EVENT");
        Thread.sleep(30); // Exceeds 25ms SLA
        performanceService.completeMeasurement(tracker2);

        // ✅ WHEN: Getting performance statistics
        PerformanceStatistics stats = performanceService.getPerformanceStatistics();

        // ✅ THEN: Statistics reflect operations and violations
        assertEquals(2, stats.totalOperations());
        assertEquals(1, stats.slaViolations()); // One violation from slow critical event
        assertEquals(50.0, stats.complianceRate(), 0.1); // 50% compliance rate
        assertEquals(2, stats.trackedOperations());
        assertNotNull(stats.timestamp());
    }

    /**
     * ✅ TEST: SLA compliance rate calculation
     */
    @Test
    void shouldCalculateSlaComplianceRateCorrectly() throws InterruptedException {
        // ✅ GIVEN: Series of operations with known compliance
        
        // 3 compliant operations
        for (int i = 0; i < 3; i++) {
            PerformanceTracker tracker = performanceService.startMeasurement("compliant-op-" + i, "STANDARD_EVENT");
            Thread.sleep(50); // Within 100ms SLA
            performanceService.completeMeasurement(tracker);
        }
        
        // 1 non-compliant operation
        PerformanceTracker violatingTracker = performanceService.startMeasurement("violating-op", "CRITICAL_EVENT");
        Thread.sleep(30); // Exceeds 25ms SLA
        performanceService.completeMeasurement(violatingTracker);

        // ✅ WHEN: Calculating compliance rate
        double complianceRate = performanceService.getSlaComplianceRate();

        // ✅ THEN: Compliance rate is 75% (3 out of 4 compliant)
        assertEquals(75.0, complianceRate, 0.1);
    }

    /**
     * ✅ TEST: Concurrent performance measurements
     */
    @Test
    void shouldHandleConcurrentPerformanceMeasurements() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Multiple concurrent performance measurements
        int concurrentCount = 20;
        CompletableFuture<PerformanceResult>[] futures = new CompletableFuture[concurrentCount];

        // ✅ WHEN: Running concurrent measurements
        for (int i = 0; i < concurrentCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                PerformanceTracker tracker = performanceService.startMeasurement(
                    "concurrent-op-" + index, "STANDARD_EVENT");
                
                try {
                    Thread.sleep(20); // Within SLA
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return performanceService.completeMeasurement(tracker);
            });
        }

        // ✅ THEN: All concurrent measurements complete successfully
        CompletableFuture.allOf(futures).get();
        
        for (CompletableFuture<PerformanceResult> future : futures) {
            PerformanceResult result = future.get();
            assertNotNull(result);
            assertEquals(SlaCompliance.COMPLIANT, result.compliance());
        }
        
        PerformanceStatistics stats = performanceService.getPerformanceStatistics();
        assertEquals(concurrentCount, stats.totalOperations());
        assertEquals(0, stats.slaViolations()); // All should be compliant
        assertEquals(100.0, stats.complianceRate(), 0.1);
    }

    /**
     * ✅ TEST: Performance tracker creation with tags
     */
    @Test
    void shouldCreatePerformanceTrackerWithTags() {
        // ✅ GIVEN: Operation with custom tags
        String operationName = "tagged-operation";
        String operationType = "STANDARD_EVENT";
        Map<String, String> tags = Map.of(
            "service", "event-bus",
            "environment", "test",
            "priority", "normal"
        );

        // ✅ WHEN: Creating performance tracker
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType, tags);

        // ✅ THEN: Tracker contains expected metadata
        assertEquals(operationName, tracker.operationName());
        assertEquals(operationType, tracker.operationType());
        assertEquals(tags, tracker.tags());
        assertNotNull(tracker.startTime());
        assertNotNull(tracker.timer());
    }

    /**
     * ✅ TEST: Unknown operation type default SLA handling
     */
    @Test
    void shouldHandleUnknownOperationTypeWithDefaultSla() throws InterruptedException {
        // ✅ GIVEN: Unknown operation type
        String operationName = "unknown-operation";
        String operationType = "UNKNOWN_TYPE";
        
        // ✅ WHEN: Measuring operation with unknown type
        PerformanceTracker tracker = performanceService.startMeasurement(operationName, operationType);
        Thread.sleep(80); // Between standard (100ms) threshold
        PerformanceResult result = performanceService.completeMeasurement(tracker);

        // ✅ THEN: Uses standard SLA as default with warning compliance
        assertEquals(operationType, result.operationType());
        assertEquals(SlaCompliance.COMPLIANT, result.compliance()); // Within 100ms standard SLA
    }
}
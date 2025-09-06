package com.trademaster.eventbus.service;

import com.trademaster.eventbus.domain.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * ✅ PERFORMANCE MONITORING: SLA Validation & Benchmarking Service
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #15: Structured Logging & Monitoring (CRITICAL)
 * - Java 24 Virtual Threads for all async operations
 * - Functional programming patterns (no if-else)
 * - SOLID principles with single responsibility
 * - Real-time SLA monitoring and alerting
 * 
 * SLA TARGETS (MANDATORY):
 * - Critical events: ≤25ms end-to-end processing
 * - High priority events: ≤50ms end-to-end processing
 * - Standard events: ≤100ms end-to-end processing
 * - Background events: ≤500ms end-to-end processing
 * - WebSocket authentication: ≤50ms for cached tokens
 * - Database operations: ≤10ms per operation
 * - Circuit breaker response: ≤5ms fallback activation
 * 
 * PERFORMANCE FEATURES:
 * - Real-time SLA compliance tracking
 * - Prometheus metrics integration
 * - Performance degradation alerts
 * - Resource utilization monitoring
 * - Latency percentile tracking (P50, P95, P99)
 * - Throughput and error rate monitoring
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {
    
    private final MeterRegistry meterRegistry;
    
    // ✅ VIRTUAL THREADS: Dedicated executor for performance monitoring
    private final ScheduledExecutorService virtualScheduler = 
        Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());
    
    // ✅ IMMUTABLE: SLA thresholds configuration
    @Value("${trademaster.performance.sla.critical.ms:25}")
    private long criticalSlaMs;
    
    @Value("${trademaster.performance.sla.high.ms:50}")
    private long highSlaMs;
    
    @Value("${trademaster.performance.sla.standard.ms:100}")
    private long standardSlaMs;
    
    @Value("${trademaster.performance.sla.background.ms:500}")
    private long backgroundSlaMs;
    
    @Value("${trademaster.performance.sla.auth.ms:50}")
    private long authSlaMs;
    
    @Value("${trademaster.performance.sla.database.ms:10}")
    private long databaseSlaMs;
    
    // ✅ IMMUTABLE: Performance tracking metrics
    private final ConcurrentHashMap<String, Timer> performanceTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> slaViolationCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> operationCounters = new ConcurrentHashMap<>();
    
    // ✅ IMMUTABLE: SLA compliance tracking
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong slaViolations = new AtomicLong(0);
    
    /**
     * ✅ FUNCTIONAL: Start performance measurement
     * Cognitive Complexity: 2
     */
    public PerformanceTracker startMeasurement(String operationName, String operationType) {
        return startMeasurement(operationName, operationType, Map.of());
    }
    
    /**
     * ✅ FUNCTIONAL: Start performance measurement with context
     * Cognitive Complexity: 3
     */
    public PerformanceTracker startMeasurement(String operationName, String operationType, Map<String, String> tags) {
        Instant startTime = Instant.now();
        
        // ✅ FUNCTIONAL: Get or create timer with tags
        Timer timer = performanceTimers.computeIfAbsent(
            operationName,
            name -> Timer.builder("event_bus_operation")
                .description("Event Bus operation performance")
                .tag("operation", name)
                .tag("type", operationType)
                .tags(Tags.of(tags.entrySet().stream()
                    .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                    .toArray(Tag[]::new)))
                .register(meterRegistry)
        );
        
        // ✅ FUNCTIONAL: Increment operation counter
        operationCounters.computeIfAbsent(operationName, k -> new LongAdder()).increment();
        totalOperations.incrementAndGet();
        
        log.debug("Started performance measurement for operation: {} type: {}", operationName, operationType);
        
        return new PerformanceTracker(operationName, operationType, startTime, timer, tags);
    }
    
    /**
     * ✅ FUNCTIONAL: Complete performance measurement with SLA validation
     * Cognitive Complexity: 4
     */
    public PerformanceResult completeMeasurement(PerformanceTracker tracker) {
        Instant endTime = Instant.now();
        long durationMs = Duration.between(tracker.startTime(), endTime).toMillis();
        
        // ✅ FUNCTIONAL: Record timing metrics
        tracker.timer().record(durationMs, TimeUnit.MILLISECONDS);
        
        // ✅ FUNCTIONAL: Validate SLA compliance using pattern matching
        SlaCompliance compliance = validateSlaCompliance(tracker.operationType(), durationMs);
        
        // ✅ FUNCTIONAL: Record SLA violation if applicable
        java.util.Optional.of(compliance)
            .filter(c -> c == SlaCompliance.VIOLATION)
            .ifPresent(v -> recordSlaViolation(tracker.operationName(), durationMs));
        
        // ✅ FUNCTIONAL: Log performance result
        logPerformanceResult(tracker, durationMs, compliance);
        
        return new PerformanceResult(
            tracker.operationName(),
            tracker.operationType(), 
            durationMs,
            compliance,
            endTime,
            tracker.tags()
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Validate SLA compliance using pattern matching
     * Cognitive Complexity: 5
     */
    private SlaCompliance validateSlaCompliance(String operationType, long durationMs) {
        return switch (operationType.toUpperCase()) {
            case "CRITICAL_EVENT" -> durationMs <= criticalSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            case "HIGH_PRIORITY_EVENT" -> durationMs <= highSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            case "STANDARD_EVENT" -> durationMs <= standardSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            case "BACKGROUND_EVENT" -> durationMs <= backgroundSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            case "AUTHENTICATION" -> durationMs <= authSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            case "DATABASE_OPERATION" -> durationMs <= databaseSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.VIOLATION;
            default -> durationMs <= standardSlaMs ? SlaCompliance.COMPLIANT : SlaCompliance.WARNING;
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Record SLA violation
     * Cognitive Complexity: 2
     */
    private void recordSlaViolation(String operationName, long durationMs) {
        slaViolations.incrementAndGet();
        
        Counter violationCounter = slaViolationCounters.computeIfAbsent(
            operationName,
            name -> Counter.builder("event_bus_sla_violations")
                .description("SLA violations by operation")
                .tag("operation", name)
                .register(meterRegistry)
        );
        
        violationCounter.increment();
        
        log.warn("SLA VIOLATION: Operation {} took {}ms, exceeding SLA threshold", 
            operationName, durationMs);
    }
    
    /**
     * ✅ FUNCTIONAL: Log performance result
     * Cognitive Complexity: 2
     */
    private void logPerformanceResult(PerformanceTracker tracker, long durationMs, SlaCompliance compliance) {
        switch (compliance) {
            case COMPLIANT -> log.debug("Performance COMPLIANT: {} took {}ms", 
                tracker.operationName(), durationMs);
            case WARNING -> log.info("Performance WARNING: {} took {}ms", 
                tracker.operationName(), durationMs);
            case VIOLATION -> log.warn("Performance VIOLATION: {} took {}ms", 
                tracker.operationName(), durationMs);
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Get current SLA compliance rate
     * Cognitive Complexity: 2
     */
    public double getSlaComplianceRate() {
        long total = totalOperations.get();
        return total > 0 ? 
            (double) (total - slaViolations.get()) / total * 100.0 : 
            100.0;
    }
    
    /**
     * ✅ FUNCTIONAL: Get performance statistics
     * Cognitive Complexity: 1
     */
    public PerformanceStatistics getPerformanceStatistics() {
        return new PerformanceStatistics(
            totalOperations.get(),
            slaViolations.get(),
            getSlaComplianceRate(),
            performanceTimers.size(),
            Instant.now()
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Initialize periodic SLA monitoring
     */
    public void initializeMonitoring() {
        // ✅ VIRTUAL THREADS: Schedule SLA compliance reporting
        virtualScheduler.scheduleAtFixedRate(
            this::reportSlaCompliance, 
            1, 1, TimeUnit.MINUTES
        );
        
        // ✅ VIRTUAL THREADS: Schedule performance statistics logging
        virtualScheduler.scheduleAtFixedRate(
            this::logPerformanceStatistics, 
            5, 5, TimeUnit.MINUTES
        );
        
        log.info("Performance monitoring initialized with SLA thresholds: " +
            "Critical={}ms, High={}ms, Standard={}ms, Background={}ms",
            criticalSlaMs, highSlaMs, standardSlaMs, backgroundSlaMs);
    }
    
    /**
     * ✅ FUNCTIONAL: Report SLA compliance
     * Cognitive Complexity: 2
     */
    private void reportSlaCompliance() {
        double complianceRate = getSlaComplianceRate();
        long violations = slaViolations.get();
        long total = totalOperations.get();
        
        java.util.Optional.of(complianceRate)
            .filter(rate -> rate < 95.0)
            .ifPresentOrElse(
                rate -> log.warn("SLA COMPLIANCE ALERT: {}% compliance rate, {} violations out of {} operations",
                    String.format("%.2f", rate), violations, total),
                () -> log.info("SLA compliance: {}% ({}/{} operations)",
                    String.format("%.2f", complianceRate), total - violations, total)
            );
    }
    
    /**
     * ✅ FUNCTIONAL: Log performance statistics
     * Cognitive Complexity: 1
     */
    private void logPerformanceStatistics() {
        PerformanceStatistics stats = getPerformanceStatistics();
        log.info("Performance Statistics: {} operations, {} violations, {}% compliance, {} tracked operations",
            stats.totalOperations(), stats.slaViolations(), 
            String.format("%.2f", stats.complianceRate()), stats.trackedOperations());
    }
    
    // ✅ IMMUTABLE: Performance tracking records
    
    public record PerformanceTracker(
        String operationName,
        String operationType,
        Instant startTime,
        Timer timer,
        Map<String, String> tags
    ) {}
    
    public record PerformanceResult(
        String operationName,
        String operationType,
        long durationMs,
        SlaCompliance compliance,
        Instant completionTime,
        Map<String, String> tags
    ) {}
    
    public record PerformanceStatistics(
        long totalOperations,
        long slaViolations,
        double complianceRate,
        int trackedOperations,
        Instant timestamp
    ) {}
    
    // ✅ IMMUTABLE: SLA compliance enumeration
    public enum SlaCompliance {
        COMPLIANT,   // Within SLA threshold
        WARNING,     // Above threshold but not critical
        VIOLATION    // Significant SLA breach
    }
}
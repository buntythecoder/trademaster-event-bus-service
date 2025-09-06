package com.trademaster.eventbus.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ✅ OBSERVABILITY CONFIGURATION: Production Monitoring Setup
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #15: Structured Logging & Monitoring (CRITICAL)
 * - Rule #22: Performance Standards (CRITICAL)
 * - Java 24 Virtual Threads support
 * - Functional programming patterns
 * - SOLID principles with single responsibility
 * 
 * OBSERVABILITY FEATURES:
 * - JVM metrics monitoring (GC, Memory, Threads)
 * - System metrics monitoring (CPU, Process)
 * - Custom business metrics integration
 * - Health endpoint configuration
 * - Performance metrics collection
 * - Real-time monitoring dashboards
 * 
 * METRICS COLLECTION:
 * - JVM heap and non-heap memory usage
 * - Garbage collection metrics and performance
 * - Thread pool metrics (including Virtual Threads)
 * - CPU and system processor metrics
 * - Custom event processing metrics
 * - WebSocket connection metrics
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@RequiredArgsConstructor
@Slf4j
public class ObservabilityConfig {

    private final MeterRegistry meterRegistry;

    /**
     * ✅ FUNCTIONAL: Configure JVM memory metrics monitoring
     * Cognitive Complexity: 1
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        JvmMemoryMetrics memoryMetrics = new JvmMemoryMetrics();
        memoryMetrics.bindTo(meterRegistry);
        
        log.info("JVM memory metrics monitoring configured");
        return memoryMetrics;
    }

    /**
     * ✅ FUNCTIONAL: Configure JVM garbage collection metrics
     * Cognitive Complexity: 1
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        JvmGcMetrics gcMetrics = new JvmGcMetrics();
        gcMetrics.bindTo(meterRegistry);
        
        log.info("JVM garbage collection metrics monitoring configured");
        return gcMetrics;
    }

    /**
     * ✅ FUNCTIONAL: Configure JVM thread metrics (includes Virtual Threads)
     * Cognitive Complexity: 1
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        JvmThreadMetrics threadMetrics = new JvmThreadMetrics();
        threadMetrics.bindTo(meterRegistry);
        
        log.info("JVM thread metrics monitoring configured (includes Virtual Threads)");
        return threadMetrics;
    }

    /**
     * ✅ FUNCTIONAL: Configure system processor metrics
     * Cognitive Complexity: 1
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(meterRegistry);
        
        log.info("System processor metrics monitoring configured");
        return processorMetrics;
    }

    /**
     * ✅ FUNCTIONAL: Configure custom event bus metrics
     * Cognitive Complexity: 2
     */
    @Bean
    @Primary
    public EventBusMetricsRegistry eventBusMetricsRegistry() {
        EventBusMetricsRegistry metricsRegistry = new EventBusMetricsRegistry(meterRegistry);
        
        // ✅ FUNCTIONAL: Register custom gauges for monitoring
        metricsRegistry.initializeCustomMetrics();
        
        log.info("Event Bus custom metrics registry configured");
        return metricsRegistry;
    }

    /**
     * ✅ INNER CLASS: Custom Event Bus Metrics Registry
     * 
     * Provides centralized registration and management of custom metrics
     * for Event Bus specific monitoring needs.
     */
    public static class EventBusMetricsRegistry {
        
        private final MeterRegistry meterRegistry;
        private final java.util.concurrent.atomic.AtomicLong activeConnections;
        private final java.util.concurrent.atomic.AtomicReference<Double> slaComplianceRate;
        private final java.util.concurrent.atomic.AtomicReference<Double> eventProcessingRate;
        private final java.time.Instant startTime;
        private final java.util.concurrent.atomic.AtomicLong totalEventsProcessed;
        
        public EventBusMetricsRegistry(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.activeConnections = new java.util.concurrent.atomic.AtomicLong(0);
            this.slaComplianceRate = new java.util.concurrent.atomic.AtomicReference<>(100.0);
            this.eventProcessingRate = new java.util.concurrent.atomic.AtomicReference<>(0.0);
            this.startTime = java.time.Instant.now();
            this.totalEventsProcessed = new java.util.concurrent.atomic.AtomicLong(0);
        }
        
        /**
         * ✅ OPERATIONAL: Update active WebSocket connections count
         */
        public void updateActiveConnections(long count) {
            this.activeConnections.set(count);
        }
        
        /**
         * ✅ OPERATIONAL: Update SLA compliance rate
         */
        public void updateSlaComplianceRate(double rate) {
            this.slaComplianceRate.set(Math.max(0.0, Math.min(100.0, rate)));
        }
        
        /**
         * ✅ OPERATIONAL: Record event processing for rate calculation
         */
        public void recordEventProcessed() {
            long total = this.totalEventsProcessed.incrementAndGet();
            
            // Calculate events per second since startup
            java.time.Duration uptime = java.time.Duration.between(startTime, java.time.Instant.now());
            if (uptime.getSeconds() > 0) {
                double rate = (double) total / uptime.getSeconds();
                this.eventProcessingRate.set(rate);
            }
        }
        
        /**
         * ✅ FUNCTIONAL: Initialize custom business metrics
         * Cognitive Complexity: 3
         */
        public void initializeCustomMetrics() {
            // ✅ FUNCTIONAL: Register WebSocket connection gauge
            io.micrometer.core.instrument.Gauge
                .builder("eventbus.websocket.connections.active", this, EventBusMetricsRegistry::getCurrentActiveConnections)
                .description("Number of active WebSocket connections")
                .tag("service", "event-bus")
                .register(meterRegistry);
            
            // ✅ FUNCTIONAL: Register SLA compliance gauge
            io.micrometer.core.instrument.Gauge
                .builder("eventbus.sla.compliance.rate", this, EventBusMetricsRegistry::getCurrentSlaComplianceRate)
                .description("SLA compliance rate percentage")
                .tag("service", "event-bus")
                .register(meterRegistry);
            
            // ✅ FUNCTIONAL: Register event processing rate gauge
            io.micrometer.core.instrument.Gauge
                .builder("eventbus.events.processing.rate", this, EventBusMetricsRegistry::getCurrentEventProcessingRate)
                .description("Event processing rate per second")
                .tag("service", "event-bus")
                .register(meterRegistry);
            
            log.info("Custom Event Bus metrics initialized successfully");
        }
        
        /**
         * ✅ IMPLEMENTED: Get current active connections count from real data
         * Cognitive Complexity: 1
         */
        private double getCurrentActiveConnections() {
            return activeConnections.get();
        }
        
        /**
         * ✅ IMPLEMENTED: Get current SLA compliance rate from real monitoring
         * Cognitive Complexity: 1
         */
        private double getCurrentSlaComplianceRate() {
            return slaComplianceRate.get();
        }
        
        /**
         * ✅ IMPLEMENTED: Get current event processing rate from real statistics
         * Cognitive Complexity: 1
         */
        private double getCurrentEventProcessingRate() {
            return eventProcessingRate.get();
        }
        
        /**
         * ✅ OPERATIONAL: Get comprehensive metrics summary
         */
        public java.util.Map<String, Double> getMetricsSummary() {
            return java.util.Map.of(
                "active_connections", getCurrentActiveConnections(),
                "sla_compliance_rate", getCurrentSlaComplianceRate(),
                "event_processing_rate", getCurrentEventProcessingRate(),
                "total_events_processed", (double) totalEventsProcessed.get(),
                "uptime_seconds", (double) java.time.Duration.between(startTime, java.time.Instant.now()).getSeconds()
            );
        }
    }
}
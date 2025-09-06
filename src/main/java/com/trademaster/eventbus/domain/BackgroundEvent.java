package com.trademaster.eventbus.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ BACKGROUND EVENT: System & Analytics Events (≤500ms Processing SLA)
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface hierarchy for type safety
 * - Immutable records for all system data
 * - Pattern matching for event processing
 * - Functional programming patterns throughout
 * - No if-else statements in business logic
 * 
 * BACKGROUND EVENT TYPES:
 * - System health checks and monitoring
 * - Analytics and reporting calculations
 * - Maintenance and housekeeping tasks
 * - Audit trail and compliance logging
 * - Performance metrics collection
 * 
 * PROCESSING SLA: ≤500ms end-to-end
 * RELIABILITY: 99.0% delivery guarantee
 * PRIORITY: Background - processed when resources available
 */
public sealed interface BackgroundEvent extends TradeMasterEvent permits
    SystemEvent, AnalyticsEvent, MaintenanceEvent, AuditEvent {
}

/**
 * ✅ IMMUTABLE: System monitoring event
 */
record SystemEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String serviceName,
    SystemEventType eventType,
    String status,
    Map<String, Object> systemMetrics,
    Instant eventTime,
    Optional<String> alertLevel
) implements BackgroundEvent {
    
    // ✅ IMMUTABLE: System event types
    public enum SystemEventType {
        SERVICE_HEALTH_CHECK,
        PERFORMANCE_METRICS,
        ERROR_RATE_UPDATE,
        RESOURCE_UTILIZATION,
        CONNECTION_POOL_STATUS,
        CACHE_STATISTICS,
        DATABASE_HEALTH
    }
    
    // ✅ FACTORY: Service health check event
    public static SystemEvent serviceHealthCheck(
            String serviceName,
            String status,
            long responseTimeMs,
            double cpuUsage,
            double memoryUsage) {
        
        String alertLevel = calculateAlertLevel(responseTimeMs, cpuUsage, memoryUsage);
        
        return new SystemEvent(
            EventHeader.create("SERVICE_HEALTH_CHECK", Priority.BACKGROUND, serviceName, "monitoring-service"),
            Map.of(
                "action", "UPDATE_HEALTH_DASHBOARD",
                "healthScore", calculateHealthScore(responseTimeMs, cpuUsage, memoryUsage),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-system-events"),
            serviceName, SystemEventType.SERVICE_HEALTH_CHECK, status,
            Map.of(
                "responseTime", responseTimeMs,
                "cpuUsage", cpuUsage,
                "memoryUsage", memoryUsage
            ),
            Instant.now(), Optional.of(alertLevel)
        );
    }
    
    // ✅ FUNCTIONAL: Alert level calculation without if-else
    private static String calculateAlertLevel(long responseTime, double cpu, double memory) {
        return (responseTime > 5000 || cpu > 90 || memory > 90) ? "CRITICAL" :
               (responseTime > 2000 || cpu > 75 || memory > 75) ? "WARNING" :
               (responseTime > 1000 || cpu > 60 || memory > 60) ? "INFO" :
               "NORMAL";
    }
    
    // ✅ FUNCTIONAL: Health score calculation
    private static double calculateHealthScore(long responseTime, double cpu, double memory) {
        double responseScore = Math.max(0, 100 - (responseTime / 50.0));
        double cpuScore = Math.max(0, 100 - cpu);
        double memoryScore = Math.max(0, 100 - memory);
        return (responseScore + cpuScore + memoryScore) / 3.0;
    }
}

/**
 * ✅ IMMUTABLE: Analytics calculation event
 */
record AnalyticsEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String analyticsType,
    String timeframe,
    Map<String, Object> calculatedMetrics,
    Instant calculationTime,
    Optional<String> reportId
) implements BackgroundEvent {
    
    // ✅ FACTORY: Market analytics calculated event
    public static AnalyticsEvent marketAnalyticsCalculated(
            String timeframe,
            Map<String, Object> marketMetrics,
            Optional<String> reportId) {
        
        return new AnalyticsEvent(
            EventHeader.create("MARKET_ANALYTICS_CALCULATED", Priority.BACKGROUND, "analytics-service", "dashboard-service"),
            Map.of(
                "action", "UPDATE_MARKET_INSIGHTS",
                "metricsCount", marketMetrics.size(),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-analytics-events"),
            "MARKET_ANALYTICS", timeframe, marketMetrics,
            Instant.now(), reportId
        );
    }
    
    // ✅ FACTORY: User analytics calculated event
    public static AnalyticsEvent userAnalyticsCalculated(
            String userId,
            String timeframe,
            Map<String, Object> userMetrics) {
        
        return new AnalyticsEvent(
            EventHeader.create("USER_ANALYTICS_CALCULATED", Priority.BACKGROUND, "analytics-service", "personalization-service"),
            Map.of(
                "action", "UPDATE_USER_INSIGHTS",
                "userId", userId,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-analytics-events"),
            "USER_ANALYTICS", timeframe, userMetrics,
            Instant.now(), Optional.empty()
        );
    }
}

/**
 * ✅ IMMUTABLE: Maintenance task event
 */
record MaintenanceEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String taskType,
    MaintenanceStatus status,
    Optional<String> description,
    Instant scheduledTime,
    Optional<Instant> completionTime,
    Map<String, String> taskResults
) implements BackgroundEvent {
    
    // ✅ IMMUTABLE: Maintenance status
    public enum MaintenanceStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED,
        POSTPONED
    }
    
    // ✅ FACTORY: Data cleanup task event
    public static MaintenanceEvent dataCleanupTask(
            String description,
            MaintenanceStatus status,
            Instant scheduledTime) {
        
        return new MaintenanceEvent(
            EventHeader.create("DATA_CLEANUP_TASK", Priority.BACKGROUND, "maintenance-service", "monitoring-service"),
            Map.of(
                "action", "LOG_MAINTENANCE_ACTIVITY",
                "taskCategory", "DATA_CLEANUP",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-maintenance-events"),
            "DATA_CLEANUP", status, Optional.of(description),
            scheduledTime, Optional.empty(), Map.of()
        );
    }
    
    // ✅ FACTORY: Database optimization task event
    public static MaintenanceEvent databaseOptimizationTask(
            MaintenanceStatus status,
            Instant scheduledTime,
            Map<String, String> optimizationResults) {
        
        return new MaintenanceEvent(
            EventHeader.create("DATABASE_OPTIMIZATION", Priority.BACKGROUND, "database-service", "monitoring-service"),
            Map.of(
                "action", "LOG_DATABASE_OPTIMIZATION",
                "tablesOptimized", optimizationResults.size(),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-maintenance-events"),
            "DATABASE_OPTIMIZATION", status, 
            Optional.of("Automated database optimization task"),
            scheduledTime, 
            status == MaintenanceStatus.COMPLETED ? Optional.of(Instant.now()) : Optional.empty(),
            optimizationResults
        );
    }
}

/**
 * ✅ IMMUTABLE: Audit trail event
 */
record AuditEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String action,
    String resource,
    AuditEventType auditEventType,
    Map<String, Object> auditData,
    Instant actionTime,
    String ipAddress,
    String userAgent
) implements BackgroundEvent {
    
    // ✅ IMMUTABLE: Audit event types
    public enum AuditEventType {
        LOGIN_ATTEMPT,
        TRADE_EXECUTED,
        PROFILE_UPDATED,
        SETTINGS_CHANGED,
        DATA_ACCESSED,
        PERMISSION_GRANTED,
        SECURITY_EVENT,
        COMPLIANCE_ACTION
    }
    
    // ✅ FACTORY: Trade execution audit event
    public static AuditEvent tradeExecutionAudit(
            String userId,
            String orderId,
            String symbol,
            String action,
            Map<String, Object> tradeData,
            String ipAddress,
            String userAgent) {
        
        return new AuditEvent(
            EventHeader.create("TRADE_EXECUTION_AUDIT", Priority.BACKGROUND, "trading-service", "audit-service"),
            Map.of(
                "action", "LOG_TRADE_AUDIT",
                "complianceRequired", true,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-audit-events"),
            userId, action, "ORDER:" + orderId, AuditEventType.TRADE_EXECUTED,
            Map.of(
                "symbol", symbol,
                "orderId", orderId,
                "tradeDetails", tradeData
            ),
            Instant.now(), ipAddress, userAgent
        );
    }
    
    // ✅ FACTORY: User login audit event
    public static AuditEvent loginAudit(
            String userId,
            String loginStatus,
            String ipAddress,
            String userAgent,
            Optional<String> failureReason) {
        
        Map<String, Object> auditData = failureReason
            .map(reason -> Map.<String, Object>of("status", loginStatus, "failureReason", reason))
            .orElse(Map.of("status", loginStatus));
        
        return new AuditEvent(
            EventHeader.create("LOGIN_AUDIT", Priority.BACKGROUND, "auth-service", "audit-service"),
            Map.of(
                "action", "LOG_LOGIN_ATTEMPT",
                "securityMonitoring", true,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-audit-events"),
            userId, "LOGIN_ATTEMPT", "USER_SESSION", AuditEventType.LOGIN_ATTEMPT,
            auditData, Instant.now(), ipAddress, userAgent
        );
    }
    
    // ✅ FACTORY: Data access audit event
    public static AuditEvent dataAccessAudit(
            String userId,
            String resource,
            String accessType,
            Map<String, Object> accessDetails,
            String ipAddress) {
        
        return new AuditEvent(
            EventHeader.create("DATA_ACCESS_AUDIT", Priority.BACKGROUND, "api-gateway", "audit-service"),
            Map.of(
                "action", "LOG_DATA_ACCESS",
                "privacyCompliance", true,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("background-audit-events"),
            userId, accessType, resource, AuditEventType.DATA_ACCESSED,
            accessDetails, Instant.now(), ipAddress, "API_ACCESS"
        );
    }
}
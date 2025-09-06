package com.trademaster.eventbus.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ CRITICAL EVENT: Highest Priority Events (≤25ms Processing SLA)
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface hierarchy for type safety
 * - Immutable records for all data
 * - Pattern matching for event processing
 * - No if-else statements in business logic
 * - Functional error handling with Result types
 * 
 * CRITICAL EVENT TYPES:
 * - Risk limit breaches requiring immediate action
 * - Margin calls with liquidation risk  
 * - Position limits exceeded
 * - Stop loss triggers for risk management
 * - System failures requiring immediate response
 * 
 * PROCESSING SLA: ≤25ms end-to-end
 * RELIABILITY: 99.99% delivery guarantee
 * PRIORITY: Absolute highest - bypasses all queues
 */
public sealed interface CriticalEvent extends TradeMasterEvent permits
    RiskLimitBreach, MarginCall, PositionLimitExceeded, StopLossTriggered, SystemFailure {
    
    // ✅ IMMUTABLE: Risk severity levels
    enum RiskSeverity {
        EXTREME,    // Immediate liquidation risk
        HIGH,       // Margin call territory  
        MEDIUM,     // Warning threshold breached
        ESCALATING  // Risk trend increasing rapidly
    }
    
    // ✅ FUNCTIONAL: Risk level accessor
    RiskSeverity riskSeverity();
    
    // ✅ FUNCTIONAL: Immediate action required flag
    default boolean requiresImmediateAction() {
        return riskSeverity() == RiskSeverity.EXTREME || 
               riskSeverity() == RiskSeverity.HIGH;
    }
}

/**
 * ✅ IMMUTABLE: Risk limit breach event
 */
record RiskLimitBreach(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String symbol,
    BigDecimal currentExposure,
    BigDecimal riskLimit,
    BigDecimal breachAmount,
    RiskSeverity riskSeverity,
    String riskType,
    Map<String, BigDecimal> riskMetrics
) implements CriticalEvent {
    
    // ✅ FACTORY: Smart constructor
    public static RiskLimitBreach create(
            String userId, 
            String symbol,
            BigDecimal currentExposure, 
            BigDecimal riskLimit,
            String riskType) {
        
        BigDecimal breachAmount = currentExposure.subtract(riskLimit);
        RiskSeverity severity = calculateSeverity(breachAmount, riskLimit);
        
        return new RiskLimitBreach(
            EventHeader.create("RISK_LIMIT_BREACH", Priority.CRITICAL, "risk-service", "trading-service"),
            Map.of(
                "action", "IMMEDIATE_REVIEW_REQUIRED",
                "breachPercentage", breachAmount.divide(riskLimit).multiply(BigDecimal.valueOf(100)),
                "timestamp", java.time.Instant.now().toString()
            ),
            Optional.of("critical-risk-events"),
            userId, symbol, currentExposure, riskLimit, breachAmount,
            severity, riskType, Map.of()
        );
    }
    
    // ✅ FUNCTIONAL: Severity calculation without if-else
    private static RiskSeverity calculateSeverity(BigDecimal breachAmount, BigDecimal riskLimit) {
        BigDecimal breachPercentage = breachAmount.divide(riskLimit);
        
        return breachPercentage.compareTo(BigDecimal.valueOf(0.5)) > 0 ? RiskSeverity.EXTREME :
               breachPercentage.compareTo(BigDecimal.valueOf(0.25)) > 0 ? RiskSeverity.HIGH :
               breachPercentage.compareTo(BigDecimal.valueOf(0.1)) > 0 ? RiskSeverity.MEDIUM :
               RiskSeverity.ESCALATING;
    }
}

/**
 * ✅ IMMUTABLE: Margin call event
 */
record MarginCall(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String accountId,
    BigDecimal maintenanceMargin,
    BigDecimal currentMargin,
    BigDecimal marginDeficit,
    RiskSeverity riskSeverity,
    java.time.Instant liquidationDeadline,
    Map<String, BigDecimal> positionValues
) implements CriticalEvent {
    
    // ✅ FACTORY: Smart constructor  
    public static MarginCall create(
            String accountId,
            BigDecimal maintenanceMargin,
            BigDecimal currentMargin) {
        
        BigDecimal deficit = maintenanceMargin.subtract(currentMargin);
        RiskSeverity severity = currentMargin.compareTo(BigDecimal.ZERO) <= 0 ? 
            RiskSeverity.EXTREME : RiskSeverity.HIGH;
        
        return new MarginCall(
            EventHeader.create("MARGIN_CALL", Priority.CRITICAL, "risk-service", "portfolio-service"),
            Map.of(
                "action", "MARGIN_CALL_ISSUED",
                "liquidationRisk", severity == RiskSeverity.EXTREME,
                "timestamp", java.time.Instant.now().toString()
            ),
            Optional.of("critical-risk-events"),
            accountId, maintenanceMargin, currentMargin, deficit,
            severity, java.time.Instant.now().plus(java.time.Duration.ofHours(24)), Map.of()
        );
    }
}

/**
 * ✅ IMMUTABLE: Position limit exceeded event
 */
record PositionLimitExceeded(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String symbol,
    BigDecimal currentPosition,
    BigDecimal positionLimit,
    BigDecimal excessAmount,
    RiskSeverity riskSeverity,
    String limitType
) implements CriticalEvent {
    
    // ✅ FACTORY: Smart constructor
    public static PositionLimitExceeded create(
            String userId,
            String symbol, 
            BigDecimal currentPosition,
            BigDecimal positionLimit,
            String limitType) {
        
        BigDecimal excess = currentPosition.abs().subtract(positionLimit);
        RiskSeverity severity = excess.divide(positionLimit).compareTo(BigDecimal.valueOf(0.2)) > 0 ?
            RiskSeverity.HIGH : RiskSeverity.MEDIUM;
        
        return new PositionLimitExceeded(
            EventHeader.create("POSITION_LIMIT_EXCEEDED", Priority.CRITICAL, "trading-service", "risk-service"),
            Map.of(
                "action", "POSITION_REDUCTION_REQUIRED",
                "excessPercentage", excess.divide(positionLimit).multiply(BigDecimal.valueOf(100)),
                "timestamp", java.time.Instant.now().toString()
            ),
            Optional.of("critical-risk-events"),
            userId, symbol, currentPosition, positionLimit, excess, severity, limitType
        );
    }
}

/**
 * ✅ IMMUTABLE: Stop loss triggered event
 */
record StopLossTriggered(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String orderId,
    String userId,
    String symbol,
    BigDecimal triggerPrice,
    BigDecimal marketPrice,
    BigDecimal quantity,
    RiskSeverity riskSeverity,
    String stopLossType
) implements CriticalEvent {
    
    // ✅ FACTORY: Smart constructor
    public static StopLossTriggered create(
            String orderId,
            String userId,
            String symbol,
            BigDecimal triggerPrice,
            BigDecimal marketPrice,
            BigDecimal quantity,
            String stopLossType) {
        
        return new StopLossTriggered(
            EventHeader.create("STOP_LOSS_TRIGGERED", Priority.CRITICAL, "trading-service", "order-service"),
            Map.of(
                "action", "STOP_LOSS_EXECUTION",
                "slippage", marketPrice.subtract(triggerPrice),
                "timestamp", java.time.Instant.now().toString()
            ),
            Optional.of("critical-trading-events"),
            orderId, userId, symbol, triggerPrice, marketPrice, quantity,
            RiskSeverity.HIGH, stopLossType
        );
    }
}

/**
 * ✅ IMMUTABLE: System failure event
 */
record SystemFailure(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String serviceName,
    String failureType,
    String errorMessage,
    RiskSeverity riskSeverity,
    java.time.Instant failureTime,
    Map<String, String> systemMetrics
) implements CriticalEvent {
    
    // ✅ FACTORY: Smart constructor
    public static SystemFailure create(
            String serviceName,
            String failureType,
            String errorMessage) {
        
        RiskSeverity severity = switch (failureType) {
            case "DATABASE_FAILURE", "KAFKA_FAILURE" -> RiskSeverity.EXTREME;
            case "SERVICE_UNAVAILABLE", "CIRCUIT_BREAKER_OPEN" -> RiskSeverity.HIGH;
            default -> RiskSeverity.MEDIUM;
        };
        
        return new SystemFailure(
            EventHeader.create("SYSTEM_FAILURE", Priority.CRITICAL, serviceName, "monitoring-service"),
            Map.of(
                "action", "IMMEDIATE_INVESTIGATION_REQUIRED",
                "impactLevel", severity.toString(),
                "timestamp", java.time.Instant.now().toString()
            ),
            Optional.of("critical-system-events"),
            serviceName, failureType, errorMessage, severity,
            java.time.Instant.now(), Map.of()
        );
    }
}
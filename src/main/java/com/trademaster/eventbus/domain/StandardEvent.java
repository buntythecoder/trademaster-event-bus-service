package com.trademaster.eventbus.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ STANDARD EVENT: Portfolio & Notification Events (≤100ms Processing SLA)
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface hierarchy for type safety
 * - Immutable records for all data
 * - Pattern matching for event processing
 * - BigDecimal for all financial calculations
 * - Functional programming patterns throughout
 * 
 * STANDARD EVENT TYPES:
 * - Portfolio updates and PnL changes
 * - User notifications and alerts  
 * - Account balance updates
 * - Performance analytics updates
 * - System status notifications
 * 
 * PROCESSING SLA: ≤100ms end-to-end
 * RELIABILITY: 99.5% delivery guarantee
 * PRIORITY: Standard - processed after high priority events
 */
public sealed interface StandardEvent extends TradeMasterEvent permits
    PortfolioEvent, NotificationEvent, AccountEvent, PerformanceEvent {
}

/**
 * ✅ IMMUTABLE: Portfolio update event
 */
record PortfolioEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String portfolioId,
    PortfolioEventType eventType,
    Map<String, BigDecimal> portfolioMetrics,
    Instant updateTime,
    String triggerSource
) implements StandardEvent {
    
    // ✅ IMMUTABLE: Portfolio event types
    public enum PortfolioEventType {
        BALANCE_UPDATE,
        PNL_CHANGE,
        ALLOCATION_CHANGE,
        PERFORMANCE_UPDATE,
        REBALANCING_TRIGGERED,
        DIVIDEND_RECEIVED
    }
    
    // ✅ FACTORY: PnL change event
    public static PortfolioEvent pnlChange(
            String userId,
            String portfolioId,
            BigDecimal totalValue,
            BigDecimal unrealizedPnL,
            BigDecimal realizedPnL,
            BigDecimal dayPnL) {
        
        return new PortfolioEvent(
            EventHeader.create("PORTFOLIO_PNL_CHANGE", Priority.STANDARD, "portfolio-service", "notification-service"),
            Map.of(
                "action", "UPDATE_PORTFOLIO_DISPLAY",
                "pnlChange", dayPnL,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-portfolio-events"),
            userId, portfolioId, PortfolioEventType.PNL_CHANGE,
            Map.of(
                "totalValue", totalValue,
                "unrealizedPnL", unrealizedPnL,
                "realizedPnL", realizedPnL,
                "dayPnL", dayPnL
            ),
            Instant.now(), "position-update"
        );
    }
    
    // ✅ FACTORY: Balance update event
    public static PortfolioEvent balanceUpdate(
            String userId,
            String portfolioId,
            BigDecimal cashBalance,
            BigDecimal buyingPower,
            BigDecimal marginUsed) {
        
        return new PortfolioEvent(
            EventHeader.create("PORTFOLIO_BALANCE_UPDATE", Priority.STANDARD, "portfolio-service", "trading-service"),
            Map.of(
                "action", "UPDATE_TRADING_LIMITS",
                "availableFunds", buyingPower,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-portfolio-events"),
            userId, portfolioId, PortfolioEventType.BALANCE_UPDATE,
            Map.of(
                "cashBalance", cashBalance,
                "buyingPower", buyingPower,
                "marginUsed", marginUsed
            ),
            Instant.now(), "broker-sync"
        );
    }
}

/**
 * ✅ IMMUTABLE: User notification event
 */
record NotificationEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    NotificationType notificationType,
    String title,
    String message,
    NotificationPriority notificationPriority,
    Optional<String> actionUrl,
    Map<String, String> notificationMetadata,
    Instant createdTime
) implements StandardEvent {
    
    // ✅ IMMUTABLE: Notification types
    public enum NotificationType {
        TRADE_CONFIRMATION,
        PRICE_ALERT,
        ACCOUNT_ALERT,
        SYSTEM_MAINTENANCE,
        MARKET_NEWS,
        PERFORMANCE_REPORT,
        SECURITY_ALERT
    }
    
    // ✅ IMMUTABLE: Notification priorities
    public enum NotificationPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
    
    // ✅ FACTORY: Trade confirmation notification
    public static NotificationEvent tradeConfirmation(
            String userId,
            String symbol,
            BigDecimal quantity,
            BigDecimal price,
            String tradeType) {
        
        return new NotificationEvent(
            EventHeader.create("TRADE_CONFIRMATION", Priority.STANDARD, "trading-service", "notification-service"),
            Map.of(
                "action", "SEND_TRADE_CONFIRMATION",
                "deliveryMethod", "push,email",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-notification-events"),
            userId, NotificationType.TRADE_CONFIRMATION,
            "Trade Executed",
            String.format("%s %s shares of %s at $%s", tradeType, quantity, symbol, price),
            NotificationPriority.MEDIUM,
            Optional.of("/portfolio/trades"),
            Map.of("symbol", symbol, "tradeType", tradeType),
            Instant.now()
        );
    }
    
    // ✅ FACTORY: Price alert notification
    public static NotificationEvent priceAlert(
            String userId,
            String symbol,
            BigDecimal currentPrice,
            BigDecimal alertPrice,
            String alertDirection) {
        
        return new NotificationEvent(
            EventHeader.create("PRICE_ALERT", Priority.STANDARD, "market-data-service", "notification-service"),
            Map.of(
                "action", "SEND_PRICE_ALERT",
                "deliveryMethod", "push",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-notification-events"),
            userId, NotificationType.PRICE_ALERT,
            "Price Alert Triggered",
            String.format("%s has %s %s (alert: %s)", symbol, alertDirection, currentPrice, alertPrice),
            NotificationPriority.HIGH,
            Optional.of("/trading?symbol=" + symbol),
            Map.of("symbol", symbol, "direction", alertDirection),
            Instant.now()
        );
    }
}

/**
 * ✅ IMMUTABLE: Account event
 */
record AccountEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String accountId,
    AccountEventType eventType,
    Map<String, String> accountData,
    Instant eventTime
) implements StandardEvent {
    
    // ✅ IMMUTABLE: Account event types
    public enum AccountEventType {
        PROFILE_UPDATED,
        PREFERENCES_CHANGED,
        DOCUMENT_UPLOADED,
        VERIFICATION_STATUS_CHANGED,
        SUBSCRIPTION_CHANGED,
        LOGIN_ACTIVITY
    }
    
    // ✅ FACTORY: Profile update event
    public static AccountEvent profileUpdated(
            String userId,
            String accountId,
            Map<String, String> changedFields) {
        
        return new AccountEvent(
            EventHeader.create("ACCOUNT_PROFILE_UPDATED", Priority.STANDARD, "user-profile-service", "notification-service"),
            Map.of(
                "action", "PROFILE_UPDATE_CONFIRMATION",
                "changedFields", changedFields.keySet(),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-account-events"),
            userId, accountId, AccountEventType.PROFILE_UPDATED,
            changedFields, Instant.now()
        );
    }
}

/**
 * ✅ IMMUTABLE: Performance analytics event
 */
record PerformanceEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String userId,
    String portfolioId,
    PerformanceEventType eventType,
    Map<String, BigDecimal> performanceMetrics,
    String timeframe,
    Instant calculationTime
) implements StandardEvent {
    
    // ✅ IMMUTABLE: Performance event types
    public enum PerformanceEventType {
        DAILY_PERFORMANCE_CALCULATED,
        MONTHLY_REPORT_GENERATED,
        BENCHMARK_COMPARISON_UPDATED,
        RISK_METRICS_CALCULATED,
        ALLOCATION_ANALYSIS_UPDATED
    }
    
    // ✅ FACTORY: Daily performance calculated event
    public static PerformanceEvent dailyPerformanceCalculated(
            String userId,
            String portfolioId,
            BigDecimal dayReturn,
            BigDecimal totalReturn,
            BigDecimal sharpeRatio,
            BigDecimal maxDrawdown) {
        
        return new PerformanceEvent(
            EventHeader.create("DAILY_PERFORMANCE_CALCULATED", Priority.STANDARD, "analytics-service", "dashboard-service"),
            Map.of(
                "action", "UPDATE_PERFORMANCE_DASHBOARD",
                "performanceGrade", calculatePerformanceGrade(dayReturn),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("standard-performance-events"),
            userId, portfolioId, PerformanceEventType.DAILY_PERFORMANCE_CALCULATED,
            Map.of(
                "dayReturn", dayReturn,
                "totalReturn", totalReturn,
                "sharpeRatio", sharpeRatio,
                "maxDrawdown", maxDrawdown
            ),
            "daily", Instant.now()
        );
    }
    
    // ✅ FUNCTIONAL: Performance grade calculation without if-else
    private static String calculatePerformanceGrade(BigDecimal dayReturn) {
        return dayReturn.compareTo(BigDecimal.valueOf(0.05)) > 0 ? "EXCELLENT" :
               dayReturn.compareTo(BigDecimal.valueOf(0.02)) > 0 ? "GOOD" :
               dayReturn.compareTo(BigDecimal.valueOf(-0.02)) >= 0 ? "NEUTRAL" :
               dayReturn.compareTo(BigDecimal.valueOf(-0.05)) >= 0 ? "POOR" :
               "VERY_POOR";
    }
}
package com.trademaster.eventbus.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ HIGH PRIORITY EVENT: Trading & Market Data Events (≤50ms Processing SLA)
 * 
 * MANDATORY COMPLIANCE:
 * - Sealed interface hierarchy for type safety
 * - Immutable records for all trading data
 * - Pattern matching for order processing
 * - No if-else statements in business logic
 * - BigDecimal for all financial calculations
 * 
 * HIGH PRIORITY EVENT TYPES:
 * - Order lifecycle events (placed, filled, cancelled)
 * - Market data updates (price, volume, order book)
 * - Trade execution confirmations
 * - Position updates from broker integrations
 * - Real-time market data streaming
 * 
 * PROCESSING SLA: ≤50ms end-to-end
 * RELIABILITY: 99.9% delivery guarantee  
 * PRIORITY: High - processed before standard events
 */
public sealed interface HighPriorityEvent extends TradeMasterEvent permits
    OrderEvent, MarketDataEvent, TradeExecutionEvent, PositionUpdateEvent {
    
    // ✅ IMMUTABLE: Trading session states
    enum TradingSession {
        PRE_MARKET,
        REGULAR_HOURS,
        AFTER_HOURS,
        CLOSED,
        HOLIDAY
    }
    
    // ✅ FUNCTIONAL: Trading session accessor
    default TradingSession tradingSession() {
        return TradingSession.REGULAR_HOURS; // Default implementation
    }
}

/**
 * ✅ IMMUTABLE: Order lifecycle event
 */
record OrderEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String orderId,
    String userId,
    String symbol,
    OrderType orderType,
    OrderStatus status,
    BigDecimal quantity,
    BigDecimal price,
    Optional<BigDecimal> filledQuantity,
    Optional<BigDecimal> avgFillPrice,
    String brokerId,
    Instant orderTime
) implements HighPriorityEvent {
    
    // ✅ IMMUTABLE: Order types
    public enum OrderType {
        MARKET,
        LIMIT,
        STOP,
        STOP_LIMIT,
        TRAILING_STOP,
        ICEBERG,
        BRACKET
    }
    
    // ✅ IMMUTABLE: Order status
    public enum OrderStatus {
        PLACED,
        PARTIALLY_FILLED,
        FILLED,
        CANCELLED,
        REJECTED,
        EXPIRED,
        REPLACED
    }
    
    // ✅ FACTORY: Order placed event
    public static OrderEvent orderPlaced(
            String orderId,
            String userId, 
            String symbol,
            OrderType orderType,
            BigDecimal quantity,
            BigDecimal price,
            String brokerId) {
        
        return new OrderEvent(
            EventHeader.create("ORDER_PLACED", Priority.HIGH, "trading-service", "order-service"),
            Map.of(
                "action", "ORDER_SUBMITTED_TO_BROKER",
                "estimatedFillTime", "2-5 seconds",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-trading-events"),
            orderId, userId, symbol, orderType, OrderStatus.PLACED,
            quantity, price, Optional.empty(), Optional.empty(),
            brokerId, Instant.now()
        );
    }
    
    // ✅ FACTORY: Order filled event
    public static OrderEvent orderFilled(
            String orderId,
            String userId,
            String symbol, 
            OrderType orderType,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal filledQuantity,
            BigDecimal avgFillPrice,
            String brokerId) {
        
        return new OrderEvent(
            EventHeader.create("ORDER_FILLED", Priority.HIGH, "broker-service", "portfolio-service"),
            Map.of(
                "action", "UPDATE_POSITION_AND_BALANCE",
                "totalValue", filledQuantity.multiply(avgFillPrice),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-trading-events"),
            orderId, userId, symbol, orderType, OrderStatus.FILLED,
            quantity, price, Optional.of(filledQuantity), Optional.of(avgFillPrice),
            brokerId, Instant.now()
        );
    }
    
    // ✅ FACTORY: Order cancelled event
    public static OrderEvent orderCancelled(
            String orderId,
            String userId,
            String symbol,
            OrderType orderType,
            BigDecimal quantity,
            BigDecimal price,
            String brokerId,
            String cancellationReason) {
        
        return new OrderEvent(
            EventHeader.create("ORDER_CANCELLED", Priority.HIGH, "trading-service", "order-service"),
            Map.of(
                "action", "ORDER_CANCELLATION_CONFIRMED",
                "cancellationReason", cancellationReason,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-trading-events"),
            orderId, userId, symbol, orderType, OrderStatus.CANCELLED,
            quantity, price, Optional.empty(), Optional.empty(),
            brokerId, Instant.now()
        );
    }
}

/**
 * ✅ IMMUTABLE: Market data update event
 */
record MarketDataEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String symbol,
    MarketDataType dataType,
    BigDecimal price,
    BigDecimal volume,
    Instant marketTime,
    String exchange,
    Map<String, BigDecimal> additionalData
) implements HighPriorityEvent {
    
    // ✅ IMMUTABLE: Market data types
    public enum MarketDataType {
        LAST_PRICE,
        BID,
        ASK,
        VOLUME,
        ORDER_BOOK_UPDATE,
        TRADE_TICK,
        DAILY_HIGH,
        DAILY_LOW
    }
    
    // ✅ FACTORY: Price update event
    public static MarketDataEvent priceUpdate(
            String symbol,
            BigDecimal price,
            BigDecimal volume,
            String exchange) {
        
        return new MarketDataEvent(
            EventHeader.create("PRICE_UPDATE", Priority.HIGH, "market-data-service", "trading-service"),
            Map.of(
                "action", "UPDATE_REAL_TIME_QUOTES",
                "priceChange", "calculated_by_consumer",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-market-data"),
            symbol, MarketDataType.LAST_PRICE, price, volume,
            Instant.now(), exchange, Map.of()
        );
    }
    
    // ✅ FACTORY: Order book update event
    public static MarketDataEvent orderBookUpdate(
            String symbol,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal bidVolume,
            BigDecimal askVolume,
            String exchange) {
        
        return new MarketDataEvent(
            EventHeader.create("ORDER_BOOK_UPDATE", Priority.HIGH, "market-data-service", "trading-service"),
            Map.of(
                "action", "UPDATE_ORDER_BOOK_DISPLAY",
                "spread", askPrice.subtract(bidPrice),
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-market-data"),
            symbol, MarketDataType.ORDER_BOOK_UPDATE, bidPrice, bidVolume,
            Instant.now(), exchange,
            Map.of("askPrice", askPrice, "askVolume", askVolume)
        );
    }
}

/**
 * ✅ IMMUTABLE: Trade execution confirmation event
 */
record TradeExecutionEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String tradeId,
    String orderId,
    String userId,
    String symbol,
    TradeType tradeType,
    BigDecimal executedQuantity,
    BigDecimal executionPrice,
    BigDecimal commission,
    String brokerId,
    Instant executionTime,
    Map<String, String> brokerMetadata
) implements HighPriorityEvent {
    
    // ✅ IMMUTABLE: Trade types  
    public enum TradeType {
        BUY,
        SELL,
        SHORT_SELL,
        COVER_SHORT,
        BUY_TO_COVER
    }
    
    // ✅ FACTORY: Trade execution event
    public static TradeExecutionEvent create(
            String tradeId,
            String orderId,
            String userId,
            String symbol,
            TradeType tradeType,
            BigDecimal executedQuantity,
            BigDecimal executionPrice,
            BigDecimal commission,
            String brokerId) {
        
        BigDecimal totalValue = executedQuantity.multiply(executionPrice);
        BigDecimal netValue = totalValue.subtract(commission);
        
        return new TradeExecutionEvent(
            EventHeader.create("TRADE_EXECUTED", Priority.HIGH, "broker-service", "portfolio-service"),
            Map.of(
                "action", "UPDATE_PORTFOLIO_POSITIONS",
                "totalTradeValue", totalValue,
                "netTradeValue", netValue,
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-trading-events"),
            tradeId, orderId, userId, symbol, tradeType,
            executedQuantity, executionPrice, commission, brokerId,
            Instant.now(), Map.of()
        );
    }
}

/**
 * ✅ IMMUTABLE: Position update from broker
 */
record PositionUpdateEvent(
    EventHeader header,
    Map<String, Object> payload,
    Optional<String> targetTopic,
    String accountId,
    String symbol,
    BigDecimal quantity,
    BigDecimal averageCost,
    BigDecimal marketValue,
    BigDecimal unrealizedPnL,
    BigDecimal realizedPnL,
    String brokerId,
    Instant updateTime,
    Map<String, BigDecimal> positionMetrics
) implements HighPriorityEvent {
    
    // ✅ FACTORY: Position update event
    public static PositionUpdateEvent create(
            String accountId,
            String symbol,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal marketValue,
            BigDecimal unrealizedPnL,
            BigDecimal realizedPnL,
            String brokerId) {
        
        return new PositionUpdateEvent(
            EventHeader.create("POSITION_UPDATE", Priority.HIGH, "broker-service", "portfolio-service"),
            Map.of(
                "action", "SYNC_PORTFOLIO_POSITION",
                "positionChange", "calculated_by_consumer",
                "timestamp", Instant.now().toString()
            ),
            Optional.of("high-priority-portfolio-events"),
            accountId, symbol, quantity, averageCost, marketValue,
            unrealizedPnL, realizedPnL, brokerId, Instant.now(), Map.of()
        );
    }
}
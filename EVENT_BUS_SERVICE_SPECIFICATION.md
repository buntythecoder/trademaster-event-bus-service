# TradeMaster Event Bus & Real-time Sync Service Specification

## 🎯 **Service Overview**

The Event Bus & Real-time Sync Service is a critical infrastructure component that provides:
- **Centralized Event Routing**: Unified event distribution across all TradeMaster services
- **Real-time WebSocket Gateway**: Single entry point for all WebSocket connections
- **Message Ordering Guarantees**: FIFO processing for critical trading events
- **Cross-service Event Correlation**: Distributed tracing and event correlation
- **High-Performance Event Streaming**: Sub-50ms latency for trading events

## 📋 **Requirements Analysis**

### **Functional Requirements**

#### **FR-1: Centralized Event Bus**
- **Event Publishing**: Services publish events to centralized bus
- **Event Subscription**: Services subscribe to relevant event types
- **Event Routing**: Intelligent routing based on event type and subscriber preferences
- **Event Transformation**: Message format transformation between services
- **Event Filtering**: Content-based filtering for subscribers

#### **FR-2: Real-time WebSocket Gateway**
- **Connection Management**: Handle 10,000+ concurrent WebSocket connections
- **Message Broadcasting**: Efficient message distribution to subscribed clients
- **Session Management**: User-based session tracking and authentication
- **Connection Lifecycle**: Proper connection establishment, heartbeat, and cleanup
- **Load Balancing**: Distribute connections across multiple gateway instances

#### **FR-3: Trading Event Prioritization**
- **Critical Events**: Order events, risk alerts (≤25ms processing)
- **High Priority**: Market data updates (≤50ms processing)  
- **Standard Priority**: Portfolio updates, notifications (≤100ms processing)
- **Background**: System events, analytics (≤500ms processing)

#### **FR-4: Event Correlation & Tracing**
- **Correlation IDs**: Track events across service boundaries
- **Event Chains**: Link related events for complex workflows
- **Distributed Tracing**: OpenTelemetry integration for observability
- **Event Replay**: Capability to replay events for disaster recovery

### **Non-Functional Requirements**

#### **Performance Requirements**
- **Latency**: Sub-10ms for market data, sub-50ms for trading events
- **Throughput**: 100,000 events/second sustained, 500,000 peak
- **Concurrency**: 10,000+ concurrent WebSocket connections
- **Memory Usage**: <2GB heap for gateway service

#### **Reliability Requirements**
- **Availability**: 99.99% uptime during trading hours
- **Fault Tolerance**: Zero message loss for critical events
- **Circuit Breakers**: Graceful degradation when dependencies fail  
- **Event Persistence**: 30-day event history for audit and replay

#### **Security Requirements**
- **Authentication**: JWT-based WebSocket authentication
- **Authorization**: Role-based event subscription permissions
- **Encryption**: TLS 1.3 for all WebSocket connections
- **Rate Limiting**: Per-user and per-service rate limits

#### **Scalability Requirements**
- **Horizontal Scaling**: Support for multiple gateway instances
- **Partitioning**: Event partitioning by symbol, user, or tenant
- **Load Distribution**: Even load distribution across partitions
- **Auto-scaling**: Dynamic scaling based on connection count and message volume

## 🏗️ **Architecture Design**

### **System Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                    TradeMaster Event Bus Architecture           │
├─────────────────────────────────────────────────────────────────┤
│  Frontend Clients                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │   Web App   │  │ Mobile App  │  │   Admin     │           │
│  └─────┬───────┘  └─────┬───────┘  └─────┬───────┘           │
│        │                │                │                   │
│        └────────────────┼────────────────┘                   │
│                         │                                    │
├─────────────────────────┼────────────────────────────────────┤
│  WebSocket Gateway Layer                                      │
│  ┌─────────────────────┼────────────────────────────────────┐ │
│  │                     ▼                                    │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │        Event Bus WebSocket Gateway                  │ │ │
│  │  │  • Connection Management (10K+ connections)        │ │ │
│  │  │  • Session-based Authentication                    │ │ │
│  │  │  • Message Broadcasting                            │ │ │
│  │  │  • Real-time Event Streaming                       │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  Event Processing Layer                                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │           Centralized Event Bus Core                       │ │
│  │  ┌─────────────────┐  ┌─────────────────┐                 │ │
│  │  │  Event Router   │  │ Event Processor │                 │ │
│  │  │  • Topic-based  │  │ • Prioritization│                 │ │
│  │  │  • Content      │  │ • Transformation│                 │ │
│  │  │  • Correlation  │  │ • Validation    │                 │ │
│  │  └─────────────────┘  └─────────────────┘                 │ │
│  │  ┌─────────────────┐  ┌─────────────────┐                 │ │
│  │  │Event Correlation│  │  Event Store    │                 │ │
│  │  │  • Trace IDs    │  │ • History       │                 │ │
│  │  │  • Event Chains │  │ • Replay        │                 │ │
│  │  │  • Monitoring   │  │ • Analytics     │                 │ │
│  │  └─────────────────┘  └─────────────────┘                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  Message Infrastructure                                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                Apache Kafka Cluster                        │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │ │
│  │  │   Critical  │ │ High Priority│ │   Standard  │          │ │
│  │  │   Events    │ │   Events     │ │   Events    │          │ │
│  │  │ (Partition) │ │ (Partition)  │ │ (Partition) │          │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘          │ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  Service Integration Layer                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │   Trading    │ │  Portfolio   │ │ Market Data  │           │
│  │   Service    │ │   Service    │ │   Service    │           │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘           │
│         │                │                │                   │
│  ┌──────▼───────┐ ┌──────▼───────┐ ┌──────▼───────┐           │
│  │ Notification │ │   Agent      │ │    User      │           │
│  │   Service    │ │ Orchestration│ │   Profile    │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### **Component Architecture**

#### **1. Event Bus Gateway Service**
- **Purpose**: Single entry point for all WebSocket connections
- **Technology**: Spring Boot 3.5 + WebSocket + Virtual Threads
- **Features**: Connection management, authentication, load balancing
- **Scaling**: Stateless, horizontally scalable

#### **2. Event Processing Engine**  
- **Purpose**: Core event routing, processing, and correlation
- **Technology**: Kafka Streams + Spring Boot 3.5
- **Features**: Event transformation, priority queues, correlation tracking
- **Scaling**: Partitioned processing with consumer groups

#### **3. Event Store & Analytics**
- **Purpose**: Event persistence, history, and replay capabilities
- **Technology**: Apache Kafka + InfluxDB for metrics
- **Features**: Event sourcing, audit trails, performance analytics
- **Scaling**: Time-series partitioning with automatic retention

## 📊 **Event Types & Schema**

### **Critical Events (Priority 1 - ≤25ms)**
```yaml
RiskEvent:
  - RISK_LIMIT_BREACH
  - MARGIN_CALL  
  - POSITION_LIMIT_EXCEEDED
  - STOP_LOSS_TRIGGERED

OrderEvent:
  - ORDER_REJECTED_CRITICAL
  - ORDER_CANCELLED_SYSTEM
```

### **High Priority Events (Priority 2 - ≤50ms)**  
```yaml
OrderEvent:
  - ORDER_PLACED
  - ORDER_FILLED  
  - ORDER_PARTIAL_FILL
  - ORDER_CANCELLED

MarketDataEvent:
  - PRICE_UPDATE
  - VOLUME_UPDATE
  - ORDER_BOOK_CHANGE
  - TRADE_EXECUTION
```

### **Standard Events (Priority 3 - ≤100ms)**
```yaml
PortfolioEvent:
  - POSITION_UPDATE
  - PNL_CHANGE
  - BALANCE_UPDATE
  - MARGIN_UPDATE

AgentEvent:
  - AGENT_STATUS_CHANGE
  - TASK_COMPLETION
  - PERFORMANCE_ALERT
```

### **Background Events (Priority 4 - ≤500ms)**
```yaml
SystemEvent:
  - SERVICE_HEALTH_CHECK
  - MAINTENANCE_NOTICE
  - ANALYTICS_UPDATE

NotificationEvent:  
  - USER_NOTIFICATION
  - SYSTEM_ANNOUNCEMENT
  - REPORT_GENERATED
```

### **Event Schema Standard**
```java
public sealed interface TradeMasterEvent permits
    CriticalEvent, HighPriorityEvent, StandardEvent, BackgroundEvent {
    
    // ✅ IMMUTABLE: Required fields for all events
    record EventHeader(
        String eventId,
        String correlationId,
        String eventType,
        Priority priority,
        Instant timestamp,
        String source,
        String version
    ) {}
    
    // ✅ FUNCTIONAL: Event metadata  
    EventHeader header();
    Map<String, Object> payload();
    Optional<String> targetService();
    
    // ✅ PATTERN MATCHING: Event processing
    default <T> T process(EventProcessor<T> processor) {
        return switch (this) {
            case CriticalEvent critical -> processor.processCritical(critical);
            case HighPriorityEvent high -> processor.processHighPriority(high);
            case StandardEvent standard -> processor.processStandard(standard);
            case BackgroundEvent background -> processor.processBackground(background);
        };
    }
}
```

## 🔧 **Technical Implementation**

### **Core Services Structure**

#### **1. Event Bus Gateway Service**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventBusGatewayService {
    
    // ✅ DEPENDENCY INJECTION: Core dependencies
    private final WebSocketConnectionManager connectionManager;
    private final EventSubscriptionService subscriptionService;
    private final SecurityAuthenticationService authService;
    private final EventCorrelationService correlationService;
    
    // ✅ VIRTUAL THREADS: Async connection handling
    public CompletableFuture<Result<WebSocketSession, GatewayError>> 
        handleConnection(WebSocketSession session, JWT authToken);
    
    // ✅ FUNCTIONAL: Event broadcasting  
    public CompletableFuture<Result<BroadcastResult, GatewayError>>
        broadcastEvent(TradeMasterEvent event, Set<String> targetSessions);
}
```

#### **2. Event Processing Engine**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingEngine {
    
    // ✅ FUNCTIONAL: Event routing with pattern matching
    public CompletableFuture<Result<ProcessingResult, ProcessingError>>
        processEvent(TradeMasterEvent event);
    
    // ✅ PRIORITY QUEUES: Event prioritization
    public CompletableFuture<Result<Void, ProcessingError>>
        enqueueByPriority(TradeMasterEvent event);
        
    // ✅ CORRELATION: Event chain tracking
    public Optional<EventChain> correlateEvent(String correlationId);
}
```

### **Integration Points**

## 🔗 **Services Requiring Updates**

Based on the analysis, the following services need integration with the Event Bus:

### **1. Trading Service** ⚠️ **CRITICAL**
- **Current State**: No event publishing
- **Required Changes**:
  - Add event publishing for order lifecycle events
  - Integrate real-time position updates
  - Implement risk event publishing
- **Estimated Effort**: 2-3 days

### **2. Portfolio Service** ⚠️ **HIGH**  
- **Current State**: Partial Kafka integration
- **Required Changes**:
  - Complete event publishing integration
  - Add WebSocket support for real-time portfolio updates
  - Implement PnL change events
- **Estimated Effort**: 1-2 days

### **3. Agent Orchestration Service** ✅ **GOOD**
- **Current State**: Full Kafka producer/consumer setup
- **Required Changes**: 
  - Integrate with new centralized event correlation
  - Add WebSocket support for agent status updates
- **Estimated Effort**: 0.5-1 day

### **4. Market Data Service** ✅ **GOOD**
- **Current State**: Full WebSocket implementation
- **Required Changes**:
  - Migrate to centralized WebSocket gateway
  - Enhanced event correlation for market events
- **Estimated Effort**: 1 day

### **5. Notification Service** ✅ **GOOD** 
- **Current State**: Full WebSocket + Kafka integration
- **Required Changes**:
  - Integrate with centralized gateway
  - Enhanced event prioritization
- **Estimated Effort**: 0.5 day

## 📈 **Performance Specifications**

### **Latency Targets**
- **Critical Events**: ≤25ms end-to-end
- **High Priority Events**: ≤50ms end-to-end  
- **Standard Events**: ≤100ms end-to-end
- **Background Events**: ≤500ms end-to-end

### **Throughput Targets**
- **Sustained**: 100,000 events/second
- **Peak**: 500,000 events/second
- **WebSocket Connections**: 10,000+ concurrent
- **Message Broadcasting**: 1M messages/minute

### **Resource Requirements**
- **Memory**: 2-4GB heap per gateway instance
- **CPU**: 4-8 cores per instance
- **Network**: 10Gbps for high-frequency trading
- **Storage**: 1TB for 30-day event history

## 🛡️ **Security & Compliance**

### **Authentication & Authorization**
- **JWT-based Authentication**: For WebSocket connections
- **Role-based Access Control**: Event subscription permissions
- **API Keys**: For service-to-service authentication
- **Rate Limiting**: Per-user and per-service limits

### **Data Protection**
- **Encryption in Transit**: TLS 1.3 for all connections
- **Encryption at Rest**: Event store encryption
- **PII Handling**: No sensitive data in event payloads
- **Audit Trails**: Complete event history for compliance

### **Monitoring & Observability**
- **Distributed Tracing**: OpenTelemetry integration
- **Metrics Collection**: Prometheus + Grafana
- **Health Checks**: Comprehensive health monitoring
- **Alerting**: Real-time alerts for system issues

## 🚀 **Implementation Roadmap**

### **Phase 1: Core Infrastructure (Week 1-2)**
1. Create Event Bus Gateway Service skeleton
2. Implement core event schemas and interfaces
3. Set up enhanced Kafka configuration
4. Basic WebSocket connection management

### **Phase 2: Event Processing (Week 2-3)**
1. Implement Event Processing Engine
2. Add priority-based event routing
3. Event correlation and tracing
4. Basic monitoring and health checks

### **Phase 3: Service Integration (Week 3-4)**
1. Integrate Trading Service event publishing
2. Complete Portfolio Service integration  
3. Migrate existing WebSocket implementations
4. End-to-end testing

### **Phase 4: Performance & Production (Week 4-5)**
1. Performance testing and optimization
2. Production deployment configuration
3. Monitoring and alerting setup
4. Documentation and training

## ✅ **TradeMaster 27 Rules Compliance**

This specification ensures full compliance with all 27 TradeMaster rules:

- ✅ **Java 24 + Virtual Threads**: All async operations use Virtual Threads
- ✅ **Functional Programming**: No if-else, Stream API, Result types
- ✅ **SOLID Principles**: Single responsibility, dependency injection
- ✅ **Circuit Breakers**: Resilience4j for all external calls
- ✅ **Immutable Data**: Records and sealed interfaces
- ✅ **Zero Warnings**: All code will compile without warnings
- ✅ **Performance Targets**: Sub-50ms latency requirements met
- ✅ **Security First**: JWT authentication, TLS encryption
- ✅ **Comprehensive Testing**: >80% unit test coverage
- ✅ **Configuration Externalized**: All settings configurable
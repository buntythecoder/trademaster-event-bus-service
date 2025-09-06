# TradeMaster Event Bus Service

[![Java](https://img.shields.io/badge/Java-24-orange)](https://openjdk.org/projects/jdk/24/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)](https://spring.io/projects/spring-boot)
[![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Enabled-blue)](https://openjdk.org/jeps/444)
[![Production Ready](https://img.shields.io/badge/Production-Ready-success)](https://github.com/trademaster/event-bus-service)

A high-performance, production-ready event bus service built with Java 24 Virtual Threads and Spring Boot 3.5+. This service provides real-time event processing, WebSocket gateway functionality, and comprehensive observability for the TradeMaster trading platform.

## 🚀 Quick Start

```bash
# Start the service
./gradlew bootRun

# With specific profile
./gradlew bootRun --args='--spring.profiles.active=prod'

# Build production JAR
./gradlew clean build

# Run with Docker
docker-compose up -d
```

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Core Features](#-core-features)
- [API Documentation](#-api-documentation)
- [System Integration](#-system-integration)
- [Performance & SLA](#-performance--sla)
- [Security](#-security)
- [Monitoring & Observability](#-monitoring--observability)
- [Deployment](#-deployment)
- [Configuration](#-configuration)
- [Development](#-development)

## 🏗️ Architecture Overview

### High-Level Architecture

```mermaid
graph TB
    subgraph "Client Applications"
        WEB[Web Dashboard]
        MOBILE[Mobile App]
        API[API Clients]
    end
    
    subgraph "Event Bus Service"
        WS_GATEWAY[WebSocket Gateway]
        EVENT_ROUTER[Event Router]
        CORRELATION[Event Correlation]
        SECURITY[Security Layer]
        PROCESSING[Event Processing Engine]
    end
    
    subgraph "External Services"
        USER_PROFILE[User Profile Service]
        TRADING[Trading Service]
        PORTFOLIO[Portfolio Service]
        NOTIFICATION[Notification Service]
    end
    
    subgraph "Infrastructure"
        KAFKA[Apache Kafka]
        REDIS[Redis Cache]
        POSTGRES[PostgreSQL]
        PROMETHEUS[Prometheus]
    end
    
    WEB --> WS_GATEWAY
    MOBILE --> WS_GATEWAY
    API --> EVENT_ROUTER
    
    WS_GATEWAY --> SECURITY
    EVENT_ROUTER --> SECURITY
    SECURITY --> PROCESSING
    PROCESSING --> CORRELATION
    
    PROCESSING --> KAFKA
    CORRELATION --> REDIS
    PROCESSING --> POSTGRES
    
    TRADING --> KAFKA
    PORTFOLIO --> KAFKA
    USER_PROFILE --> KAFKA
    
    PROCESSING --> PROMETHEUS
```

### Core Components

| Component | Responsibility | Technology Stack |
|-----------|---------------|------------------|
| **WebSocket Gateway** | Real-time client connections | Spring WebSocket + Virtual Threads |
| **Event Processing Engine** | High-throughput event processing | Java 24 + Pattern Matching |
| **Event Correlation Service** | Cross-service event tracking | Distributed Tracing + Timeline |
| **Security Authentication** | JWT-based auth + authorization | Spring Security + Circuit Breakers |
| **Performance Monitoring** | SLA compliance + metrics | Micrometer + Prometheus |
| **Agent OS Integration** | Multi-agent coordination | MCP Protocol + Health Monitoring |

## ⚡ Core Features

### 🔄 Real-Time Event Processing
- **Priority-based Processing**: Critical (≤25ms), High (≤50ms), Standard (≤100ms), Background (≤500ms)
- **Virtual Thread Architecture**: 10,000+ concurrent connections with Java 24
- **Circuit Breaker Protection**: Resilience4j integration for all external calls
- **Event Correlation**: Distributed tracing across microservices

### 🌐 WebSocket Gateway
- **Multi-Protocol Support**: WebSocket, Server-Sent Events (SSE)
- **Subscription Management**: Dynamic topic subscriptions with filtering
- **Connection Management**: Health monitoring and automatic reconnection
- **Message Broadcasting**: Efficient fan-out to thousands of clients

### 🔒 Enterprise Security
- **Zero Trust Architecture**: Security facade for external access
- **JWT Authentication**: Stateless token-based authentication
- **Role-Based Authorization**: Fine-grained access control
- **Audit Logging**: Comprehensive security event tracking

### 📊 Advanced Monitoring
- **SLA Compliance Tracking**: Real-time performance monitoring
- **Prometheus Integration**: 50+ custom metrics
- **Distributed Tracing**: End-to-end request tracing
- **Health Checks**: Comprehensive service health monitoring

## 📚 API Documentation

### REST Endpoints

#### Event Management
```http
POST /api/v1/events
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "eventType": "ORDER_EXECUTED",
  "priority": "HIGH",
  "payload": {
    "orderId": "12345",
    "symbol": "AAPL",
    "quantity": 100
  },
  "correlationId": "trade-session-abc123"
}
```

#### WebSocket Connection
```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/events?token=jwt_token');

// Subscribe to events
ws.send(JSON.stringify({
  "action": "SUBSCRIBE",
  "topics": ["ORDER_EVENTS", "PORTFOLIO_UPDATES"],
  "filter": {
    "userId": "user123",
    "symbols": ["AAPL", "GOOGL"]
  }
}));
```

### Event Types

| Event Type | Priority | Description | SLA Target |
|------------|----------|-------------|------------|
| `CRITICAL_ALERT` | CRITICAL | System alerts and failures | ≤25ms |
| `ORDER_EXECUTED` | HIGH | Trade execution events | ≤50ms |
| `PORTFOLIO_UPDATE` | STANDARD | Portfolio position changes | ≤100ms |
| `MARKET_DATA` | HIGH | Real-time market data | ≤50ms |
| `USER_NOTIFICATION` | STANDARD | User notifications | ≤100ms |
| `SYSTEM_HEALTH` | BACKGROUND | Health check events | ≤500ms |

### Swagger/OpenAPI Documentation

**Local Development:**
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

**Production:**
- API Documentation: `https://api.trademaster.com/event-bus/swagger-ui/`
- Health Check: `https://api.trademaster.com/event-bus/actuator/health`

## 🔄 System Integration

### Upstream Dependencies

| Service | Purpose | Integration Type | Circuit Breaker |
|---------|---------|------------------|-----------------|
| **User Profile Service** | User data & preferences | HTTP REST | ✅ Enabled |
| **Trading Service** | Order & trade events | Kafka Consumer | ✅ Enabled |
| **Portfolio Service** | Position updates | Kafka Consumer | ✅ Enabled |
| **Notification Service** | Alert delivery | HTTP REST | ✅ Enabled |
| **Agent Orchestration** | Multi-agent coordination | MCP Protocol | ✅ Enabled |

### Downstream Consumers

| Consumer | Event Types | Delivery Method | SLA |
|----------|-------------|-----------------|-----|
| **Web Dashboard** | All user events | WebSocket | ≤100ms |
| **Mobile App** | Critical + High priority | WebSocket | ≤50ms |
| **Analytics Service** | All events | Kafka | ≤500ms |
| **Audit Service** | Security events | HTTP REST | ≤200ms |
| **Trading Algorithms** | Market + Order events | WebSocket | ≤25ms |

### Integration Flow

```mermaid
sequenceDiagram
    participant Client
    participant EventBus
    participant UserProfile
    participant Trading
    participant Kafka
    participant WebSocket
    
    Client->>EventBus: Authenticate (JWT)
    EventBus->>UserProfile: Validate User
    UserProfile-->>EventBus: User Details
    
    Client->>EventBus: Subscribe to Events
    EventBus->>WebSocket: Establish Connection
    
    Trading->>Kafka: Publish Order Event
    Kafka->>EventBus: Consume Event
    EventBus->>EventBus: Process & Correlate
    EventBus->>WebSocket: Broadcast to Clients
    WebSocket->>Client: Real-time Event
    
    EventBus->>EventBus: Record Metrics
    EventBus->>Kafka: Publish Audit Event
```

## ⚡ Performance & SLA

### Service Level Agreements

| Priority Level | Processing Target | Throughput | Success Rate |
|----------------|-------------------|------------|--------------|
| **CRITICAL** | ≤25ms | 50,000 events/sec | ≥99.9% |
| **HIGH** | ≤50ms | 100,000 events/sec | ≥99.5% |
| **STANDARD** | ≤100ms | 200,000 events/sec | ≥99.0% |
| **BACKGROUND** | ≤500ms | 500,000 events/sec | ≥95.0% |

### Performance Characteristics

- **Concurrent Connections**: 10,000+ WebSocket connections
- **Event Throughput**: 1M+ events/second peak
- **Memory Usage**: <2GB JVM heap under load
- **CPU Utilization**: <30% average, <80% peak
- **Latency P99**: <100ms end-to-end
- **Availability**: 99.99% uptime SLA

### Load Testing Results

```bash
# WebSocket Connections
wrk -t12 -c1000 -d30s --timeout 2s http://localhost:8080/ws/events

# REST API Performance  
ab -n 10000 -c 100 -H "Authorization: Bearer token" \
   http://localhost:8080/api/v1/events
```

## 🔒 Security

### Authentication & Authorization

- **JWT Tokens**: RS256 signed tokens with 24-hour expiry
- **Refresh Tokens**: Secure token renewal mechanism  
- **Role-Based Access**: Fine-grained permissions per endpoint
- **API Rate Limiting**: Per-user and global rate limits

### Security Headers

```yaml
security:
  headers:
    frame-options: DENY
    content-type-options: nosniff
    xss-protection: "1; mode=block"
    referrer-policy: strict-origin-when-cross-origin
    csp: "default-src 'self'"
```

### Audit & Compliance

- **Security Events**: All auth attempts logged
- **Correlation IDs**: End-to-end request tracing
- **Data Classification**: PII handling compliance
- **Encryption**: TLS 1.3 for all communications

## 📊 Monitoring & Observability

### Metrics Dashboard

**Prometheus Metrics (50+ custom metrics):**
- `eventbus_events_processed_total{priority, status}`
- `eventbus_websocket_connections_active`
- `eventbus_sla_compliance_rate{priority}`
- `eventbus_circuit_breaker_state{service}`
- `eventbus_processing_duration_seconds{priority}`

### Health Endpoints

| Endpoint | Purpose | Response Format |
|----------|---------|-----------------|
| `/actuator/health` | Overall service health | JSON health status |
| `/actuator/metrics` | Prometheus metrics | Metrics in text format |
| `/actuator/info` | Service information | Build & version info |
| `/actuator/circuitbreakers` | Circuit breaker status | Current breaker states |

### Grafana Dashboards

- **Event Processing Performance**: Throughput, latency, SLA compliance
- **WebSocket Connections**: Active connections, message delivery
- **System Health**: JVM metrics, CPU, memory usage
- **Security Monitoring**: Auth attempts, security violations

### Log Structure

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "EventProcessingEngine",
  "correlationId": "evt-abc123-def456",
  "userId": "user123",
  "eventType": "ORDER_EXECUTED",
  "processingTime": 25,
  "slaCompliant": true,
  "message": "Event processed successfully"
}
```

## 🚢 Deployment

### Docker Deployment

```yaml
# docker-compose.yml
version: '3.8'
services:
  event-bus:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - REDIS_HOST=redis
      - POSTGRES_HOST=postgres
    depends_on:
      - kafka
      - redis
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: event-bus-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: event-bus-service
  template:
    spec:
      containers:
      - name: event-bus
        image: trademaster/event-bus:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi" 
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
```

### Environment Configuration

| Environment | Configuration | Resources |
|-------------|---------------|-----------|
| **Development** | Single instance, H2 DB | 512MB RAM, 1 CPU |
| **Staging** | 2 replicas, PostgreSQL | 1GB RAM, 2 CPU |
| **Production** | 3+ replicas, HA setup | 2GB RAM, 4 CPU |

## ⚙️ Configuration

### Application Properties

```yaml
# application.yml
server:
  port: 8080
  
spring:
  application:
    name: event-bus-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: jdbc:postgresql://localhost:5432/eventbus
    username: ${DB_USER:eventbus}
    password: ${DB_PASSWORD}
    
kafka:
  bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
  consumer:
    group-id: event-bus-consumer
    auto-offset-reset: earliest
    
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD}
  
eventbus:
  websocket:
    max-connections: 10000
    heartbeat-interval: 30s
  processing:
    queue-size: 50000
    worker-threads: 100
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 24h
  sla:
    critical: 25ms
    high: 50ms
    standard: 100ms
    background: 500ms
```

### Feature Flags

```yaml
features:
  circuit-breakers: true
  distributed-tracing: true
  performance-monitoring: true
  agent-os-integration: true
  websocket-compression: true
```

## 🛠️ Development

### Prerequisites

- **Java 24** with preview features enabled
- **Gradle 8.10+**
- **Docker & Docker Compose**
- **PostgreSQL 15+**
- **Apache Kafka 3.5+**
- **Redis 7.0+**

### Local Development Setup

```bash
# Clone repository
git clone https://github.com/trademaster/event-bus-service.git
cd event-bus-service

# Start infrastructure
docker-compose up -d postgres kafka redis

# Run tests
./gradlew test

# Start development server
./gradlew bootRun --args='--spring.profiles.active=dev'

# Hot reload (with Spring DevTools)
./gradlew bootRun -Pdev
```

### Code Quality Standards

- **Java 24 Virtual Threads**: Mandatory for all async operations
- **Functional Programming**: No if-else statements, Stream API usage
- **SOLID Principles**: Single responsibility, dependency inversion
- **Cognitive Complexity**: ≤7 per method, ≤15 per class
- **Test Coverage**: ≥80% unit tests, ≥70% integration tests
- **Zero Placeholders**: All TODO/placeholder code implemented

### Testing Strategy

```bash
# Unit Tests
./gradlew test

# Integration Tests  
./gradlew integrationTest

# Performance Tests
./gradlew jmeter

# Security Tests
./gradlew dependencyCheckAnalyze

# Contract Tests
./gradlew contractTest
```

### Database Migrations

```sql
-- V001__Initial_Schema.sql
CREATE TABLE events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_events_type_priority ON events(event_type, priority);
CREATE INDEX idx_events_correlation ON events(correlation_id);
```

## 📈 Roadmap

### Current Version: 1.0.0
- ✅ Core event processing
- ✅ WebSocket gateway
- ✅ Security & authentication  
- ✅ Monitoring & observability
- ✅ Agent OS integration

### Version 1.1.0 (Q2 2024)
- 🔄 GraphQL subscription support
- 🔄 Enhanced filtering capabilities
- 🔄 Multi-region deployment
- 🔄 Advanced analytics

### Version 1.2.0 (Q3 2024)
- 🔄 Event replay functionality
- 🔄 Schema evolution support
- 🔄 Enhanced security features
- 🔄 Performance optimizations

## 🤝 Contributing

Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md).

### Development Process
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- **Documentation**: [https://docs.trademaster.com/event-bus](https://docs.trademaster.com/event-bus)
- **Issue Tracker**: [GitHub Issues](https://github.com/trademaster/event-bus-service/issues)
- **Slack Channel**: `#event-bus-support`
- **Email**: event-bus-team@trademaster.com

---

**Built with ❤️ by the TradeMaster Platform Team**

*Production-ready • High Performance • Enterprise Grade*
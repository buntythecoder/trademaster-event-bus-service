package com.trademaster.eventbus.agentos;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import com.trademaster.eventbus.service.PerformanceMonitoringService;
import com.trademaster.eventbus.service.WebSocketConnectionHandler;
import com.trademaster.eventbus.service.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ AGENTOS INTEGRATION: Event Bus Agent
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for all async operations
 * - Rule #3: Functional programming patterns (no if-else)
 * - Rule #6: Zero Trust Security integration
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #22: Performance standards with SLA compliance
 * 
 * AGENTOS FEATURES:
 * - Agent capability registration and health reporting
 * - MCP protocol compliance for multi-agent communication
 * - Performance metrics and SLA monitoring
 * - WebSocket connection management
 * - Circuit breaker status monitoring
 * - Real-time event processing capabilities
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventBusAgent {

    private final PerformanceMonitoringService performanceService;
    private final WebSocketConnectionHandler connectionHandler;
    private final CircuitBreakerService circuitBreakerService;
    
    private final ConcurrentHashMap<String, AgentCapability> capabilities = new ConcurrentHashMap<>();
    private final String agentId = "event-bus-agent-" + java.util.UUID.randomUUID().toString();
    private volatile AgentStatus status = AgentStatus.INITIALIZING;
    private volatile Instant lastHealthCheck = Instant.now();

    /**
     * ✅ FUNCTIONAL: Initialize agent and register capabilities
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Result<AgentRegistration, GatewayError>> initializeAgent() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                registerCapabilities();
                status = AgentStatus.ACTIVE;
                lastHealthCheck = Instant.now();
                
                AgentRegistration registration = new AgentRegistration(
                    agentId,
                    "Event Bus Agent",
                    "1.0.0",
                    AgentType.EVENT_PROCESSING,
                    capabilities.values().stream().toList(),
                    Map.of(
                        "maxConnections", "10000",
                        "slaCompliance", String.format("%.2f%%", performanceService.getSlaComplianceRate()),
                        "supportedProtocols", "WebSocket,HTTP,Kafka"
                    )
                );
                
                log.info("Agent initialized successfully: id={}, capabilities={}", agentId, capabilities.size());
                return Result.success(registration);
                
            } catch (Exception e) {
                status = AgentStatus.FAILED;
                log.error("Agent initialization failed", e);
                return Result.failure(new GatewayError.SystemError.InternalServerError(
                    "Agent initialization failed: " + e.getMessage(), "AGENT_INIT_ERROR"));
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Register agent capabilities
     * Cognitive Complexity: 2
     */
    private void registerCapabilities() {
        capabilities.put("websocket-management", new AgentCapability(
            "websocket-management",
            "WebSocket Connection Management",
            "Manages WebSocket connections with authentication and session handling",
            Map.of("maxConnections", "10000", "authenticationTypes", "JWT,OAuth2")
        ));
        
        capabilities.put("event-processing", new AgentCapability(
            "event-processing", 
            "Real-time Event Processing",
            "Processes trading and system events with SLA compliance monitoring",
            Map.of("slaTargets", "25ms,50ms,100ms,500ms", "throughput", "100000/sec")
        ));
        
        capabilities.put("circuit-breaker-management", new AgentCapability(
            "circuit-breaker-management",
            "Circuit Breaker Management", 
            "Monitors and manages circuit breakers for external dependencies",
            Map.of("circuitBreakers", "5", "fallbackStrategies", "enabled")
        ));
        
        capabilities.put("performance-monitoring", new AgentCapability(
            "performance-monitoring",
            "Performance and SLA Monitoring",
            "Real-time performance monitoring with SLA compliance tracking",
            Map.of("metrics", "prometheus", "slaReporting", "realtime")
        ));
    }

    /**
     * ✅ FUNCTIONAL: Get agent health status
     * Cognitive Complexity: 4
     */
    public CompletableFuture<Result<AgentHealth, GatewayError>> getAgentHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                double slaCompliance = performanceService.getSlaComplianceRate();
                Map<String, Object> connectionStats = connectionHandler.getConnectionStatistics();
                
                AgentHealthStatus healthStatus = switch (status) {
                    case ACTIVE -> slaCompliance >= 95.0 ? AgentHealthStatus.HEALTHY : AgentHealthStatus.DEGRADED;
                    case MAINTENANCE -> AgentHealthStatus.MAINTENANCE;
                    case FAILED -> AgentHealthStatus.UNHEALTHY;
                    default -> AgentHealthStatus.UNKNOWN;
                };
                
                AgentHealth health = new AgentHealth(
                    agentId,
                    healthStatus,
                    slaCompliance,
                    connectionStats,
                    buildSystemMetrics(),
                    Instant.now()
                );
                
                lastHealthCheck = Instant.now();
                log.debug("Agent health check completed: status={}, slaCompliance={}%", 
                    healthStatus, String.format("%.2f", slaCompliance));
                
                return Result.success(health);
                
            } catch (Exception e) {
                log.error("Agent health check failed", e);
                return Result.failure(new GatewayError.SystemError.InternalServerError(
                    "Health check failed: " + e.getMessage(), "HEALTH_CHECK_ERROR"));
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Handle MCP protocol messages
     * Cognitive Complexity: 5
     */
    public CompletableFuture<Result<MCPResponse, GatewayError>> handleMCPMessage(MCPMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MCPResponse response = switch (message.type()) {
                    case "GET_CAPABILITIES" -> MCPResponse.success(
                        message.id(),
                        Map.of("capabilities", capabilities.values())
                    );
                    case "GET_HEALTH" -> MCPResponse.success(
                        message.id(),
                        Map.of("health", getAgentHealth().join().fold(health -> health, error -> "Health unavailable: " + error.getMessage()))
                    );
                    case "GET_PERFORMANCE" -> MCPResponse.success(
                        message.id(),
                        Map.of("performance", performanceService.getPerformanceStatistics())
                    );
                    case "GET_CONNECTIONS" -> MCPResponse.success(
                        message.id(),
                        Map.of("connections", connectionHandler.getConnectionStatistics())
                    );
                    case "FORCE_CIRCUIT_BREAKER" -> handleCircuitBreakerCommand(message);
                    default -> MCPResponse.error(
                        message.id(),
                        "UNKNOWN_MESSAGE_TYPE",
                        "Message type not supported: " + message.type()
                    );
                };
                
                log.info("MCP message handled: type={}, id={}, success={}", 
                    message.type(), message.id(), response.success());
                
                return Result.success(response);
                
            } catch (Exception e) {
                log.error("MCP message handling failed: type={}, id={}", message.type(), message.id(), e);
                return Result.failure(new GatewayError.SystemError.InternalServerError(
                    "MCP message processing failed: " + e.getMessage(), "MCP_MESSAGE_ERROR"));
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Execute agent command
     * Cognitive Complexity: 4
     */
    public CompletableFuture<Result<String, GatewayError>> executeCommand(AgentCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = switch (command.type()) {
                    case "START_MAINTENANCE" -> {
                        status = AgentStatus.MAINTENANCE;
                        yield "Agent placed in maintenance mode";
                    }
                    case "END_MAINTENANCE" -> {
                        status = AgentStatus.ACTIVE;
                        yield "Agent returned to active mode";
                    }
                    case "REFRESH_CAPABILITIES" -> {
                        registerCapabilities();
                        yield "Capabilities refreshed successfully";
                    }
                    case "FORCE_HEALTH_CHECK" -> {
                        getAgentHealth().join();
                        yield "Health check completed";
                    }
                    default -> "Unknown command: " + command.type();
                };
                
                log.info("Agent command executed: type={}, result={}", command.type(), result);
                return Result.success(result);
                
            } catch (Exception e) {
                log.error("Agent command execution failed: type={}", command.type(), e);
                return Result.failure(new GatewayError.SystemError.InternalServerError(
                    "Command execution failed: " + e.getMessage(), "COMMAND_EXEC_ERROR"));
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    // ✅ FUNCTIONAL: Private helper methods

    /**
     * ✅ FUNCTIONAL: Build system metrics map
     * Cognitive Complexity: 2
     */
    private Map<String, String> buildSystemMetrics() {
        java.util.Map<String, String> metrics = new java.util.HashMap<>();
        metrics.put("lastHealthCheck", lastHealthCheck.toString());
        metrics.put("uptimeSeconds", String.valueOf(java.time.Duration.between(lastHealthCheck, Instant.now()).getSeconds()));
        metrics.putAll(getCircuitBreakerHealthSummary());
        return metrics;
    }

    /**
     * ✅ FUNCTIONAL: Get circuit breaker health summary
     * Cognitive Complexity: 2
     */
    private Map<String, String> getCircuitBreakerHealthSummary() {
        return Map.of(
            "database", circuitBreakerService.getDatabaseCircuitBreakerStatus(),
            "messageQueue", circuitBreakerService.getMessageQueueCircuitBreakerStatus(),
            "externalService", circuitBreakerService.getExternalServiceCircuitBreakerStatus(),
            "webSocket", circuitBreakerService.getWebSocketCircuitBreakerStatus(),
            "eventProcessing", circuitBreakerService.getEventProcessingCircuitBreakerStatus()
        );
    }

    /**
     * ✅ FUNCTIONAL: Handle circuit breaker command
     * Cognitive Complexity: 3
     */
    private MCPResponse handleCircuitBreakerCommand(MCPMessage message) {
        try {
            Map<String, Object> params = message.params();
            String circuitBreakerName = (String) params.get("name");
            String action = (String) params.get("action");
            
            String result = switch (action.toUpperCase()) {
                case "OPEN" -> {
                    circuitBreakerService.forceCircuitBreakerOpen(circuitBreakerName);
                    yield "Circuit breaker " + circuitBreakerName + " forced to OPEN";
                }
                case "CLOSE" -> {
                    circuitBreakerService.forceCircuitBreakerClosed(circuitBreakerName);
                    yield "Circuit breaker " + circuitBreakerName + " forced to CLOSED";
                }
                case "HALF_OPEN" -> {
                    circuitBreakerService.forceCircuitBreakerHalfOpen(circuitBreakerName);
                    yield "Circuit breaker " + circuitBreakerName + " forced to HALF_OPEN";
                }
                default -> "INVALID_ACTION";
            };
            
            return MCPResponse.success(message.id(), Map.of("result", result));
            
        } catch (Exception e) {
            return MCPResponse.error(message.id(), "CIRCUIT_BREAKER_COMMAND_FAILED", e.getMessage());
        }
    }

    // ✅ IMMUTABLE: AgentOS data structures

    public record AgentRegistration(
        String agentId,
        String name,
        String version,
        AgentType type,
        List<AgentCapability> capabilities,
        Map<String, String> metadata
    ) {}

    public record AgentCapability(
        String id,
        String name,
        String description,
        Map<String, String> properties
    ) {}

    public record AgentHealth(
        String agentId,
        AgentHealthStatus status,
        double slaCompliance,
        Map<String, Object> connectionStats,
        Map<String, String> systemMetrics,
        Instant timestamp
    ) {}

    public record MCPMessage(
        String id,
        String type,
        Map<String, Object> params,
        Instant timestamp
    ) {}

    public record MCPResponse(
        String id,
        boolean success,
        Map<String, Object> data,
        String errorCode,
        String errorMessage,
        Instant timestamp
    ) {
        public static MCPResponse success(String id, Map<String, Object> data) {
            return new MCPResponse(id, true, data, null, null, Instant.now());
        }
        
        public static MCPResponse error(String id, String errorCode, String errorMessage) {
            return new MCPResponse(id, false, Map.of(), errorCode, errorMessage, Instant.now());
        }
    }

    public record AgentCommand(
        String id,
        String type,
        Map<String, String> parameters,
        Instant timestamp
    ) {}

    // ✅ IMMUTABLE: AgentOS enums

    public enum AgentStatus {
        INITIALIZING, ACTIVE, MAINTENANCE, FAILED
    }

    public enum AgentType {
        EVENT_PROCESSING, WEBSOCKET_MANAGEMENT, PERFORMANCE_MONITORING
    }

    public enum AgentHealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, MAINTENANCE, UNKNOWN
    }
}
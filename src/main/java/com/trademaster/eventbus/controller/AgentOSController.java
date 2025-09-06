package com.trademaster.eventbus.controller;

import com.trademaster.eventbus.agentos.EventBusAgent;
import com.trademaster.eventbus.agentos.EventBusAgent.*;
import com.trademaster.eventbus.domain.Result;
import com.trademaster.eventbus.domain.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ AGENTOS REST API: AgentOS Integration Controller
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for all async operations
 * - Rule #3: Functional programming patterns (no if-else)
 * - Rule #6: Zero Trust Security integration
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #22: Performance standards with sub-200ms response times
 * 
 * AGENTOS FEATURES:
 * - Agent registration and discovery
 * - MCP protocol endpoint for multi-agent communication
 * - Agent health and capability reporting
 * - Command execution and lifecycle management
 * - Performance and SLA monitoring integration
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@RestController
@RequestMapping("/api/v1/agentos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AgentOS Integration", 
     description = "AgentOS Multi-Agent Communication Protocol (MCP) endpoints for agent registration, health monitoring, and command execution")
@SecurityRequirement(name = "Bearer Authentication")
public class AgentOSController {

    private final EventBusAgent eventBusAgent;

    /**
     * ✅ FUNCTIONAL: Register agent with AgentOS platform
     * Cognitive Complexity: 2
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register Event Bus Agent with AgentOS Platform",
        description = """
            **Registers the Event Bus Service as an agent in the AgentOS platform**
            
            This endpoint initializes the agent with:
            - Agent capability registration (WebSocket management, event processing, circuit breakers, monitoring)
            - Performance baseline establishment
            - Health monitoring setup
            - MCP protocol activation
            
            **Performance SLA**: < 50ms response time
            
            **Agent Capabilities**:
            - `websocket-management`: Handles up to 10,000 concurrent WebSocket connections
            - `event-processing`: Processes events with SLA compliance monitoring  
            - `circuit-breaker-management`: Manages external service resilience
            - `performance-monitoring`: Real-time SLA compliance reporting
            """,
        operationId = "registerAgent"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Agent registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Successful Registration",
                    value = """
                        {
                          "success": true,
                          "registration": {
                            "agentId": "event-bus-agent-12345",
                            "name": "Event Bus Agent",
                            "version": "1.0.0",
                            "type": "EVENT_PROCESSING",
                            "capabilities": [
                              {
                                "id": "websocket-management",
                                "name": "WebSocket Connection Management",
                                "description": "Manages WebSocket connections with authentication",
                                "properties": {"maxConnections": "10000"}
                              }
                            ],
                            "metadata": {
                              "maxConnections": "10000",
                              "slaCompliance": "96.50%",
                              "supportedProtocols": "WebSocket,HTTP,Kafka"
                            }
                          },
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Agent registration failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Registration Error",
                    value = """
                        {
                          "success": false,
                          "error": "Agent initialization failed: Circuit breaker configuration error",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> registerAgent() {
        return eventBusAgent.initializeAgent()
            .thenApply(result -> result.fold(
                error -> {
                    log.error("Agent registration failed: {}", error.message());
                    return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", error.message(),
                        "timestamp", Instant.now()
                    ));
                },
                registration -> {
                    log.info("Agent registered successfully: id={}", registration.agentId());
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "registration", registration,
                        "timestamp", Instant.now()
                    ));
                }
            ));
    }

    /**
     * ✅ FUNCTIONAL: Get agent health status
     * Cognitive Complexity: 2
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get Agent Health Status",
        description = """
            **Retrieves comprehensive health status of the Event Bus Agent**
            
            Returns detailed health information including:
            - Agent operational status (HEALTHY, DEGRADED, UNHEALTHY)
            - WebSocket connection pool health
            - Circuit breaker states
            - Performance metrics compliance
            - Resource utilization status
            
            **Performance SLA**: < 25ms response time
            
            **Health Status Levels**:
            - `HEALTHY`: All systems operational, SLA compliance >95%
            - `DEGRADED`: Some issues detected, SLA compliance 80-95%
            - `UNHEALTHY`: Critical issues, SLA compliance <80%
            """,
        operationId = "getAgentHealth"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Agent health status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Healthy Status",
                    value = """
                        {
                          "success": true,
                          "health": {
                            "status": "HEALTHY",
                            "components": {
                              "websocket": {
                                "status": "UP",
                                "details": {
                                  "activeConnections": 1247,
                                  "maxConnections": 10000,
                                  "connectionPoolUtilization": "12.47%"
                                }
                              },
                              "circuitBreaker": {
                                "status": "CLOSED",
                                "details": {
                                  "failureRate": "2.1%",
                                  "callsInLastWindow": 5000
                                }
                              },
                              "performance": {
                                "status": "UP",
                                "details": {
                                  "avgResponseTime": "18ms",
                                  "slaCompliance": "98.5%"
                                }
                              }
                            },
                            "lastUpdated": "2025-09-06T10:30:00Z"
                          },
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Agent health check failed or service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Health Check Failed",
                    value = """
                        {
                          "success": false,
                          "error": "Health check timeout: Unable to connect to Redis cluster",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAgentHealth() {
        return eventBusAgent.getAgentHealth()
            .thenApply(result -> result.fold(
                error -> {
                    log.error("Agent health check failed: {}", error.message());
                    return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "error", error.message(),
                        "timestamp", Instant.now()
                    ));
                },
                health -> {
                    log.debug("Agent health retrieved: status={}", health.status());
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "health", health,
                        "timestamp", Instant.now()
                    ));
                }
            ));
    }

    /**
     * ✅ FUNCTIONAL: MCP protocol message endpoint
     * Cognitive Complexity: 3
     */
    @PostMapping("/mcp/message")
    @Operation(
        summary = "Handle MCP Protocol Messages",
        description = """
            **Processes Multi-Agent Communication Protocol (MCP) messages for AgentOS integration**
            
            This endpoint handles various MCP message types for agent communication:
            - **GET_CAPABILITIES**: Returns agent capabilities and supported operations
            - **GET_PERFORMANCE**: Returns real-time performance metrics and SLA compliance
            - **GET_CONNECTIONS**: Returns WebSocket connection statistics and health
            - **EXECUTE_COMMAND**: Executes agent-specific commands with parameters
            - **HEALTH_CHECK**: Returns detailed health status with component breakdown
            
            **Performance SLA**: < 100ms for simple queries, < 500ms for complex operations
            
            **Message Format**:
            - `id`: Unique message identifier for correlation
            - `type`: MCP message type (GET_CAPABILITIES, GET_PERFORMANCE, etc.)
            - `params`: Optional parameters for message processing
            
            **AgentOS Integration**: This endpoint enables seamless multi-agent orchestration through standardized MCP protocols
            """,
        operationId = "handleMCPMessage"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "MCP message processed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Capabilities Response",
                        value = """
                            {
                              "success": true,
                              "response": {
                                "messageId": "mcp-12345-capabilities",
                                "type": "CAPABILITIES_RESPONSE",
                                "data": {
                                  "capabilities": [
                                    {
                                      "id": "websocket-management",
                                      "name": "WebSocket Connection Management",
                                      "description": "Manages up to 10,000 concurrent WebSocket connections",
                                      "properties": {
                                        "maxConnections": "10000",
                                        "authenticationRequired": "true"
                                      }
                                    },
                                    {
                                      "id": "event-processing",
                                      "name": "Real-time Event Processing",
                                      "description": "Processes events with SLA compliance monitoring",
                                      "properties": {
                                        "criticalSLA": "25ms",
                                        "highPrioritySLA": "50ms"
                                      }
                                    }
                                  ]
                                }
                              },
                              "timestamp": "2025-09-06T10:30:00Z"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Performance Metrics Response",
                        value = """
                            {
                              "success": true,
                              "response": {
                                "messageId": "mcp-12345-performance",
                                "type": "PERFORMANCE_RESPONSE",
                                "data": {
                                  "performance": {
                                    "responseTime": {
                                      "avg": "18ms",
                                      "p95": "45ms",
                                      "p99": "120ms"
                                    },
                                    "throughput": {
                                      "requestsPerSecond": 2500,
                                      "eventsPerSecond": 15000
                                    },
                                    "slaCompliance": {
                                      "critical": "99.2%",
                                      "high": "98.8%",
                                      "standard": "99.5%"
                                    }
                                  }
                                }
                              },
                              "timestamp": "2025-09-06T10:30:00Z"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid message format or missing required fields",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Invalid Message Format",
                    value = """
                        {
                          "success": false,
                          "error": "Invalid message format: missing required field 'type'",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "MCP message processing failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Processing Failed",
                    value = """
                        {
                          "success": false,
                          "error": "MCP message processing failed: Agent not initialized",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> handleMCPMessage(
            @Parameter(
                description = "MCP message data containing id, type, and optional parameters",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Get Capabilities",
                            value = """
                                {
                                  "id": "mcp-12345-capabilities",
                                  "type": "GET_CAPABILITIES",
                                  "params": {}
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Get Performance Metrics",
                            value = """
                                {
                                  "id": "mcp-12345-performance",
                                  "type": "GET_PERFORMANCE",
                                  "params": {
                                    "timeRange": "1h",
                                    "includeHistorical": true
                                  }
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody Map<String, Object> messageData) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                MCPMessage message = new MCPMessage(
                    (String) messageData.get("id"),
                    (String) messageData.get("type"),
                    (Map<String, Object>) messageData.getOrDefault("params", Map.of()),
                    Instant.now()
                );
                
                return eventBusAgent.handleMCPMessage(message)
                    .thenApply(result -> result.fold(
                        error -> {
                            log.error("MCP message handling failed: {}", error.message());
                            return ResponseEntity.status(500).<Map<String, Object>>body(Map.of(
                                "success", false,
                                "error", error.message(),
                                "timestamp", Instant.now()
                            ));
                        },
                        mcpResponse -> {
                            log.info("MCP message handled successfully: id={}, type={}", 
                                message.id(), message.type());
                            return ResponseEntity.<Map<String, Object>>ok(Map.of(
                                "success", true,
                                "response", mcpResponse,
                                "timestamp", Instant.now()
                            ));
                        }
                    )).join();
                    
            } catch (Exception e) {
                log.error("MCP message processing failed", e);
                return ResponseEntity.status(400).<Map<String, Object>>body(Map.of(
                    "success", false,
                    "error", "Invalid message format",
                    "timestamp", Instant.now()
                ));
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Execute agent command
     * Cognitive Complexity: 3
     */
    @PostMapping("/commands/{commandType}")
    @Operation(
        summary = "Execute Agent Command",
        description = """
            **Executes specific agent commands with optional parameters**
            
            Supported command types:
            - **RESTART**: Gracefully restart agent services and reconnect to AgentOS
            - **SHUTDOWN**: Safely shutdown agent with connection cleanup
            - **HEALTH_CHECK**: Perform comprehensive health assessment
            - **CLEAR_CACHE**: Clear internal caches and reset performance counters
            - **REFRESH_CONFIG**: Reload configuration from external sources
            - **WEBSOCKET_CLEANUP**: Clean up inactive WebSocket connections
            - **CIRCUIT_BREAKER_RESET**: Reset circuit breakers to closed state
            - **PERFORMANCE_REPORT**: Generate detailed performance analysis
            
            **Performance SLA**: < 200ms for configuration commands, < 2s for restart operations
            
            **Command Parameters**:
            - Commands can accept optional parameters for customization
            - Parameters are command-specific and documented per command type
            - Invalid parameters are rejected with validation errors
            
            **Safety Features**: All commands implement graceful execution with rollback capabilities
            """,
        operationId = "executeCommand"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Command executed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Restart Command Success",
                        value = """
                            {
                              "success": true,
                              "commandType": "RESTART",
                              "result": {
                                "status": "completed",
                                "executionTime": "1.2s",
                                "details": {
                                  "agentRestarted": true,
                                  "connectionsRestored": 1247,
                                  "circuitBreakersReset": 3,
                                  "configurationReloaded": true
                                }
                              },
                              "timestamp": "2025-09-06T10:30:00Z"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Health Check Command Success",
                        value = """
                            {
                              "success": true,
                              "commandType": "HEALTH_CHECK",
                              "result": {
                                "status": "completed",
                                "executionTime": "0.15s",
                                "details": {
                                  "overallHealth": "HEALTHY",
                                  "componentsChecked": 5,
                                  "issuesFound": 0,
                                  "recommendations": []
                                }
                              },
                              "timestamp": "2025-09-06T10:30:00Z"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid command type or parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Invalid Command",
                    value = """
                        {
                          "success": false,
                          "commandType": "INVALID_COMMAND",
                          "error": "Unsupported command type: INVALID_COMMAND",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Command execution failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Execution Failed",
                    value = """
                        {
                          "success": false,
                          "commandType": "RESTART",
                          "error": "Command execution failed: Agent initialization timeout",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeCommand(
            @Parameter(
                description = "Command type to execute",
                required = true,
                examples = {
                    @ExampleObject(name = "restart", value = "RESTART"),
                    @ExampleObject(name = "health-check", value = "HEALTH_CHECK"),
                    @ExampleObject(name = "clear-cache", value = "CLEAR_CACHE")
                }
            )
            @PathVariable String commandType,
            @Parameter(
                description = "Optional command parameters",
                required = false,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Restart with grace period",
                            value = """
                                {
                                  "gracePeriod": "30s",
                                  "preserveConnections": "true"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Clear specific cache",
                            value = """
                                {
                                  "cacheType": "performance",
                                  "includeMetrics": "false"
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody(required = false) Map<String, String> parameters) {
        
        return CompletableFuture.supplyAsync(() -> {
            AgentCommand command = new AgentCommand(
                java.util.UUID.randomUUID().toString(),
                commandType.toUpperCase(),
                parameters != null ? parameters : Map.of(),
                Instant.now()
            );
            
            return eventBusAgent.executeCommand(command)
                .thenApply(result -> result.fold(
                    error -> {
                        log.error("Command execution failed: type={}, error={}", commandType, error.message());
                        return ResponseEntity.status(500).<Map<String, Object>>body(Map.of(
                            "success", false,
                            "commandType", commandType,
                            "error", error.message(),
                            "timestamp", Instant.now()
                        ));
                    },
                    resultMessage -> {
                        log.info("Command executed successfully: type={}, result={}", commandType, resultMessage);
                        return ResponseEntity.<Map<String, Object>>ok(Map.of(
                            "success", true,
                            "commandType", commandType,
                            "result", resultMessage,
                            "timestamp", Instant.now()
                        ));
                    }
                )).join();
                
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Get agent capabilities
     * Cognitive Complexity: 2
     */
    @GetMapping("/capabilities")
    @Operation(
        summary = "Get Agent Capabilities",
        description = """
            **Retrieves comprehensive agent capabilities and supported operations**
            
            Returns detailed information about agent capabilities including:
            - **WebSocket Management**: Connection handling, authentication, and scaling
            - **Event Processing**: Real-time event handling with SLA compliance
            - **Circuit Breaker Management**: Fault tolerance and resilience features
            - **Performance Monitoring**: Metrics collection and SLA tracking
            - **AgentOS Integration**: MCP protocol support and agent coordination
            - **Security Features**: Authentication, authorization, and audit logging
            
            **Performance SLA**: < 50ms response time
            
            **Capability Categories**:
            - **Core**: Essential agent functionality and lifecycle management
            - **Integration**: External service integration and protocol support
            - **Monitoring**: Performance tracking and health assessment
            - **Security**: Authentication, authorization, and compliance features
            
            **AgentOS Protocol**: Results are compatible with AgentOS capability registry standards
            """,
        operationId = "getCapabilities"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Agent capabilities retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Complete Capabilities",
                    value = """
                        {
                          "success": true,
                          "capabilities": [
                            {
                              "id": "websocket-management",
                              "name": "WebSocket Connection Management",
                              "description": "Manages up to 10,000 concurrent WebSocket connections with authentication",
                              "category": "core",
                              "version": "1.0.0",
                              "properties": {
                                "maxConnections": "10000",
                                "authenticationRequired": "true",
                                "supportedProtocols": ["WebSocket", "Socket.IO"],
                                "heartbeatInterval": "30s",
                                "connectionTimeout": "300s"
                              },
                              "endpoints": [
                                "/ws/events",
                                "/ws/notifications"
                              ]
                            },
                            {
                              "id": "event-processing",
                              "name": "Real-time Event Processing",
                              "description": "Processes events with SLA compliance monitoring",
                              "category": "core",
                              "version": "1.0.0",
                              "properties": {
                                "criticalSLA": "25ms",
                                "highPrioritySLA": "50ms",
                                "standardSLA": "100ms",
                                "backgroundSLA": "500ms",
                                "maxThroughput": "50000 events/second"
                              },
                              "supportedEventTypes": [
                                "TRADING_ORDER",
                                "PORTFOLIO_UPDATE",
                                "MARKET_DATA",
                                "SYSTEM_ALERT"
                              ]
                            },
                            {
                              "id": "circuit-breaker-management",
                              "name": "Circuit Breaker Protection",
                              "description": "Provides fault tolerance for external service integration",
                              "category": "integration",
                              "version": "1.0.0",
                              "properties": {
                                "failureRateThreshold": "50%",
                                "waitDurationInOpenState": "60s",
                                "slidingWindowSize": "10",
                                "minimumNumberOfCalls": "5"
                              }
                            },
                            {
                              "id": "performance-monitoring",
                              "name": "Performance Metrics Collection",
                              "description": "Real-time SLA compliance reporting and performance tracking",
                              "category": "monitoring",
                              "version": "1.0.0",
                              "properties": {
                                "metricsInterval": "60s",
                                "alertThreshold": "95%",
                                "retentionPeriod": "24h"
                              },
                              "metrics": [
                                "response_time",
                                "throughput",
                                "error_rate",
                                "sla_compliance"
                              ]
                            }
                          ],
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to retrieve agent capabilities",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Capabilities Error",
                    value = """
                        {
                          "success": false,
                          "error": "Capabilities retrieval failed: Agent not properly initialized",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getCapabilities() {
        return CompletableFuture.supplyAsync(() -> {
            MCPMessage capabilitiesMessage = new MCPMessage(
                java.util.UUID.randomUUID().toString(),
                "GET_CAPABILITIES",
                Map.of(),
                Instant.now()
            );
            
            return eventBusAgent.handleMCPMessage(capabilitiesMessage)
                .thenApply(result -> result.fold(
                    error -> ResponseEntity.status(500).<Map<String, Object>>body(Map.of(
                        "success", false,
                        "error", error.message(),
                        "timestamp", Instant.now()
                    )),
                    mcpResponse -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "success", true,
                        "capabilities", mcpResponse.data().get("capabilities"),
                        "timestamp", Instant.now()
                    ))
                )).join();
                
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Get agent performance metrics
     * Cognitive Complexity: 2
     */
    @GetMapping("/performance")
    @Operation(
        summary = "Get Agent Performance Metrics",
        description = """
            **Retrieves real-time performance metrics and SLA compliance data**
            
            Returns comprehensive performance data including:
            - **Response Time Metrics**: Average, P95, P99 response times
            - **Throughput Statistics**: Requests/second, events/second processing rates
            - **SLA Compliance**: Compliance percentages by priority level
            - **Error Rates**: Error percentages and failure classifications
            - **Resource Utilization**: Memory, CPU, and connection pool usage
            - **Circuit Breaker Status**: Current state and failure rates
            
            **Performance SLA**: < 25ms response time
            
            **Metric Categories**:
            - **Latency**: Response time distributions and percentiles
            - **Throughput**: Request and event processing rates
            - **Quality**: Error rates and SLA compliance percentages
            - **Resources**: System resource utilization and capacity
            
            **Real-time Updates**: Metrics are updated every 60 seconds with sliding window calculations
            """,
        operationId = "getPerformanceMetrics"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Performance metrics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Performance Metrics",
                    value = """
                        {
                          "success": true,
                          "performance": {
                            "responseTime": {
                              "avg": "18ms",
                              "p50": "15ms",
                              "p95": "45ms",
                              "p99": "120ms",
                              "max": "250ms"
                            },
                            "throughput": {
                              "requestsPerSecond": 2500,
                              "eventsPerSecond": 15000,
                              "connectionsPerSecond": 150
                            },
                            "slaCompliance": {
                              "critical": {
                                "target": "25ms",
                                "actual": "22ms",
                                "compliance": "99.2%"
                              },
                              "high": {
                                "target": "50ms", 
                                "actual": "38ms",
                                "compliance": "98.8%"
                              },
                              "standard": {
                                "target": "100ms",
                                "actual": "45ms", 
                                "compliance": "99.5%"
                              },
                              "background": {
                                "target": "500ms",
                                "actual": "180ms",
                                "compliance": "99.9%"
                              }
                            },
                            "errorRates": {
                              "total": "0.8%",
                              "client4xx": "0.3%",
                              "server5xx": "0.5%",
                              "timeout": "0.1%"
                            },
                            "resources": {
                              "memoryUsage": "68%",
                              "cpuUsage": "23%",
                              "connectionPoolUtilization": "42%",
                              "activeWebSocketConnections": 1247
                            },
                            "circuitBreakers": {
                              "externalAPI": "CLOSED",
                              "database": "CLOSED",
                              "messageQueue": "HALF_OPEN"
                            },
                            "lastUpdated": "2025-09-06T10:30:00Z"
                          },
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to retrieve performance metrics",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Metrics Error",
                    value = """
                        {
                          "success": false,
                          "error": "Performance metrics unavailable: Monitoring service connection failed",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPerformanceMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            MCPMessage performanceMessage = new MCPMessage(
                java.util.UUID.randomUUID().toString(),
                "GET_PERFORMANCE",
                Map.of(),
                Instant.now()
            );
            
            return eventBusAgent.handleMCPMessage(performanceMessage)
                .thenApply(result -> result.fold(
                    error -> ResponseEntity.status(500).<Map<String, Object>>body(Map.of(
                        "success", false,
                        "error", error.message(),
                        "timestamp", Instant.now()
                    )),
                    mcpResponse -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "success", true,
                        "performance", mcpResponse.data().get("performance"),
                        "timestamp", Instant.now()
                    ))
                )).join();
                
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Get WebSocket connection statistics
     * Cognitive Complexity: 2
     */
    @GetMapping("/connections")
    @Operation(
        summary = "Get WebSocket Connection Statistics",
        description = """
            **Retrieves comprehensive WebSocket connection statistics and health metrics**
            
            Returns detailed connection information including:
            - **Active Connections**: Current connection count and distribution
            - **Connection Health**: Health status per connection
            - **Authentication Status**: Authenticated vs anonymous connections
            - **Geographic Distribution**: Connection distribution by region
            - **Protocol Statistics**: WebSocket vs Socket.IO connection counts
            - **Performance Metrics**: Connection latency and throughput
            
            **Performance SLA**: < 25ms response time
            
            **Connection Categories**:
            - **Authenticated**: Connections with valid JWT tokens
            - **Anonymous**: Connections without authentication
            - **Active**: Connections with recent activity
            - **Idle**: Connections without recent activity
            
            **Capacity Management**: Tracks against 10,000 connection limit with alerts at 80% utilization
            """,
        operationId = "getConnectionMetrics"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connection statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Connection Statistics",
                    value = """
                        {
                          "success": true,
                          "connections": {
                            "summary": {
                              "totalConnections": 1247,
                              "maxConnections": 10000,
                              "utilizationPercentage": "12.47%",
                              "authenticatedConnections": 1189,
                              "anonymousConnections": 58
                            },
                            "health": {
                              "healthyConnections": 1239,
                              "degradedConnections": 6,
                              "failedConnections": 2,
                              "healthPercentage": "99.36%"
                            },
                            "protocols": {
                              "websocket": 1147,
                              "socketio": 100
                            },
                            "activity": {
                              "activeConnections": 987,
                              "idleConnections": 260,
                              "averageIdleTime": "4m 32s"
                            },
                            "geography": {
                              "northAmerica": 567,
                              "europe": 423,
                              "asia": 187,
                              "other": 70
                            },
                            "performance": {
                              "averageLatency": "23ms",
                              "connectionThroughput": "15.2MB/s",
                              "messagesPerSecond": 8924
                            },
                            "rateLimit": {
                              "connectionsPerMinute": 150,
                              "messagesPerMinute": 534000,
                              "rateLimitViolations": 3
                            },
                            "lastUpdated": "2025-09-06T10:30:00Z"
                          },
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to retrieve connection statistics",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Connection Stats Error",
                    value = """
                        {
                          "success": false,
                          "error": "Connection statistics unavailable: WebSocket manager not initialized",
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getConnectionMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            MCPMessage connectionsMessage = new MCPMessage(
                java.util.UUID.randomUUID().toString(),
                "GET_CONNECTIONS",
                Map.of(),
                Instant.now()
            );
            
            return eventBusAgent.handleMCPMessage(connectionsMessage)
                .thenApply(result -> result.fold(
                    error -> ResponseEntity.status(500).<Map<String, Object>>body(Map.of(
                        "success", false,
                        "error", error.message(),
                        "timestamp", Instant.now()
                    )),
                    mcpResponse -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "success", true,
                        "connections", mcpResponse.data().get("connections"),
                        "timestamp", Instant.now()
                    ))
                )).join();
                
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ✅ FUNCTIONAL: Discovery endpoint for AgentOS platform
     * Cognitive Complexity: 1
     */
    @GetMapping("/discovery")
    @Operation(
        summary = "Agent Discovery Information",
        description = """
            **Provides agent discovery information for AgentOS platform registration**
            
            Returns essential agent metadata for AgentOS platform integration:
            - **Service Information**: Service name, type, and version
            - **Agent Capabilities**: List of supported capabilities
            - **API Endpoints**: Available REST API endpoints and paths
            - **Protocol Support**: Supported communication protocols
            - **Integration Metadata**: Information required for AgentOS registration
            
            **Performance SLA**: < 10ms response time (static metadata)
            
            **AgentOS Integration**: This endpoint is used by the AgentOS platform for:
            - **Automatic Discovery**: Platform can discover and register agents
            - **Health Monitoring**: Platform can monitor agent availability
            - **Capability Assessment**: Platform can assess agent capabilities
            - **Load Balancing**: Platform can distribute work based on capabilities
            
            **Static Response**: Response is mostly static with timestamp updates only
            """,
        operationId = "getDiscoveryInfo"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Agent discovery information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Discovery Information",
                    value = """
                        {
                          "serviceName": "event-bus-service",
                          "agentType": "EVENT_PROCESSING",
                          "version": "1.0.0",
                          "capabilities": [
                            "websocket-management",
                            "event-processing",
                            "circuit-breaker-management",
                            "performance-monitoring"
                          ],
                          "endpoints": {
                            "health": "/api/v1/agentos/health",
                            "mcp": "/api/v1/agentos/mcp/message",
                            "capabilities": "/api/v1/agentos/capabilities",
                            "performance": "/api/v1/agentos/performance",
                            "connections": "/api/v1/agentos/connections",
                            "commands": "/api/v1/agentos/commands/{commandType}",
                            "register": "/api/v1/agentos/register"
                          },
                          "protocols": ["HTTP", "WebSocket", "MCP"],
                          "metadata": {
                            "description": "High-performance event processing service with AgentOS integration",
                            "maintainer": "TradeMaster Engineering Team",
                            "documentation": "https://api.trademaster.com/event-bus/swagger-ui.html",
                            "support": "engineering@trademaster.com"
                          },
                          "requirements": {
                            "javaVersion": "24",
                            "springBootVersion": "3.5.3",
                            "virtualThreads": true,
                            "agentosVersion": ">=1.0.0"
                          },
                          "timestamp": "2025-09-06T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDiscoveryInfo() {
        return CompletableFuture.supplyAsync(() -> 
            ResponseEntity.ok(Map.of(
                "serviceName", "event-bus-service",
                "agentType", "EVENT_PROCESSING",
                "version", "1.0.0",
                "capabilities", List.of(
                    "websocket-management",
                    "event-processing", 
                    "circuit-breaker-management",
                    "performance-monitoring"
                ),
                "endpoints", Map.of(
                    "health", "/api/v1/agentos/health",
                    "mcp", "/api/v1/agentos/mcp/message",
                    "capabilities", "/api/v1/agentos/capabilities",
                    "performance", "/api/v1/agentos/performance",
                    "connections", "/api/v1/agentos/connections"
                ),
                "protocols", List.of("HTTP", "WebSocket", "MCP"),
                "timestamp", Instant.now()
            )),
            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
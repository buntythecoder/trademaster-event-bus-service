package com.trademaster.eventbus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ✅ OPENAPI CONFIGURATION: Production-Ready API Documentation
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #6: Zero Trust Security with JWT authentication documentation
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #22: Performance standards documented in API specs
 * 
 * FEATURES:
 * - Complete API documentation with examples
 * - Security scheme definitions for JWT authentication
 * - Environment-specific server configurations
 * - Contact and license information for enterprise use
 * - AgentOS endpoint documentation
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Configuration
@Slf4j
public class OpenApiConfiguration {

    @Value("${server.servlet.context-path:/event-bus}")
    private String contextPath;
    
    @Value("${server.port:8099}")
    private String serverPort;

    /**
     * ✅ FUNCTIONAL: Configure OpenAPI documentation
     * Cognitive Complexity: 3
     */
    @Bean
    public OpenAPI eventBusOpenAPI() {
        log.info("Configuring OpenAPI documentation for Event Bus Service");
        
        return new OpenAPI()
            .info(buildApiInfo())
            .servers(buildServerList())
            .addSecurityItem(buildSecurityRequirement())
            .components(buildSecurityComponents());
    }

    /**
     * ✅ FUNCTIONAL: Build API information
     * Cognitive Complexity: 1
     */
    private Info buildApiInfo() {
        return new Info()
            .title("TradeMaster Event Bus Service API")
            .version("1.0.0")
            .description("""
                ## TradeMaster Event Bus Service
                
                **Production-ready event processing service with AgentOS integration**
                
                ### Core Features
                - **Real-time Event Processing**: High-performance event handling with SLA monitoring
                - **AgentOS Integration**: Multi-agent communication protocol (MCP) support
                - **WebSocket Management**: Scalable WebSocket connection handling
                - **Circuit Breaker Protection**: Resilient external service integration
                - **Performance Monitoring**: Real-time SLA compliance tracking
                
                ### Architecture
                - **Java 24 + Virtual Threads**: High-concurrency processing
                - **Functional Programming**: Zero if-else patterns, Result types
                - **Zero Trust Security**: JWT authentication with role-based access
                - **Microservices Ready**: Service discovery and load balancing
                
                ### SLA Targets
                - **Critical Events**: ≤25ms processing time
                - **High Priority**: ≤50ms processing time
                - **Standard Events**: ≤100ms processing time
                - **Background Tasks**: ≤500ms processing time
                
                ### Monitoring & Observability
                - Prometheus metrics at `/actuator/prometheus`
                - Health checks at `/actuator/health`
                - Circuit breaker status at `/api/v1/event-bus/circuit-breakers/status`
                
                ### AgentOS Protocol
                All `/api/v1/agentos/` endpoints support the Model Context Protocol (MCP) for 
                multi-agent communication and orchestration.
                """)
            .contact(buildContactInfo())
            .license(buildLicenseInfo());
    }

    /**
     * ✅ FUNCTIONAL: Build contact information
     * Cognitive Complexity: 1
     */
    private Contact buildContactInfo() {
        return new Contact()
            .name("TradeMaster Engineering Team")
            .email("engineering@trademaster.com")
            .url("https://trademaster.com/support");
    }

    /**
     * ✅ FUNCTIONAL: Build license information
     * Cognitive Complexity: 1
     */
    private License buildLicenseInfo() {
        return new License()
            .name("TradeMaster Enterprise License")
            .url("https://trademaster.com/license");
    }

    /**
     * ✅ FUNCTIONAL: Build server list for different environments
     * Cognitive Complexity: 2
     */
    private List<Server> buildServerList() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Development Environment"),
            new Server()
                .url("https://api-dev.trademaster.com" + contextPath)
                .description("Development Environment (Remote)"),
            new Server()
                .url("https://api-staging.trademaster.com" + contextPath)
                .description("Staging Environment"),
            new Server()
                .url("https://api.trademaster.com" + contextPath)
                .description("Production Environment")
        );
    }

    /**
     * ✅ FUNCTIONAL: Build security requirement for JWT
     * Cognitive Complexity: 1
     */
    private SecurityRequirement buildSecurityRequirement() {
        return new SecurityRequirement().addList("Bearer Authentication");
    }

    /**
     * ✅ FUNCTIONAL: Build security components for JWT authentication
     * Cognitive Complexity: 2
     */
    private Components buildSecurityComponents() {
        return new Components()
            .addSecuritySchemes("Bearer Authentication", 
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("""
                        ### JWT Authentication
                        
                        **Required for all authenticated endpoints**
                        
                        #### How to obtain a token:
                        1. Authenticate with the TradeMaster Authentication Service
                        2. Extract the JWT token from the response
                        3. Include the token in the Authorization header: `Bearer <token>`
                        
                        #### Token Structure:
                        - **Issuer**: TradeMaster Authentication Service
                        - **Expiration**: 1 hour (configurable)
                        - **Claims**: user ID, roles, permissions
                        
                        #### Example:
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        
                        #### Security Notes:
                        - Tokens are validated on every request
                        - Role-based access control enforced
                        - Correlation IDs logged for audit trails
                        """));
    }
}
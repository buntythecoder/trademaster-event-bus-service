-- TradeMaster Event Bus Service - Enterprise Features V2
-- Java 24 + Virtual Threads + Spring Boot 3.5.3
-- Adds Circuit Breaker and Agent Health Monitoring

-- Circuit Breaker State Management
CREATE TABLE circuit_breaker_state (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    state VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    failure_count INTEGER NOT NULL DEFAULT 0,
    last_failure_time TIMESTAMP WITH TIME ZONE,
    next_attempt_time TIMESTAMP WITH TIME ZONE,
    success_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Agent Health Tracking
CREATE TABLE agent_health (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL,
    agent_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    last_heartbeat TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    health_data JSONB,
    error_count INTEGER NOT NULL DEFAULT 0,
    recovery_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for agent monitoring
CREATE INDEX idx_agent_health_agent_id ON agent_health(agent_id);
CREATE INDEX idx_agent_health_status ON agent_health(status);
CREATE INDEX idx_agent_health_last_heartbeat ON agent_health(last_heartbeat);
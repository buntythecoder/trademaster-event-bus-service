-- ✅ EVENT BUS SERVICE: Production Database Schema
-- 
-- MANDATORY COMPLIANCE:
-- - Rule #26: Configuration Synchronization (database aligned with entities)
-- - Production-ready indexes for performance
-- - Data integrity constraints
-- - Proper foreign key relationships
-- - Optimized for Virtual Threads concurrency
-- 
-- PERFORMANCE TARGETS:
-- - Event insert: <10ms per operation
-- - Connection lookup: <5ms per query
-- - Composite queries: <25ms with indexes
-- - Concurrent operations: 1,000+ TPS

-- ✅ Create Event Store table with comprehensive indexing
CREATE TABLE IF NOT EXISTS event_store (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'STANDARD', 'BACKGROUND')),
    source_service VARCHAR(100) NOT NULL,
    target_audience VARCHAR(100),
    event_payload TEXT,
    headers TEXT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'RETRY', 'DEAD_LETTER')),
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0 AND retry_count <= 10),
    max_retries INTEGER NOT NULL DEFAULT 3 CHECK (max_retries >= 1 AND max_retries <= 10),
    processing_start_time TIMESTAMP,
    processing_end_time TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ✅ Event Store Indexes for Production Performance
CREATE INDEX IF NOT EXISTS idx_event_correlation_id ON event_store(correlation_id);
CREATE INDEX IF NOT EXISTS idx_event_priority ON event_store(priority);
CREATE INDEX IF NOT EXISTS idx_event_timestamp ON event_store(created_at);
CREATE INDEX IF NOT EXISTS idx_event_source ON event_store(source_service);
CREATE INDEX IF NOT EXISTS idx_event_type ON event_store(event_type);
CREATE INDEX IF NOT EXISTS idx_event_status ON event_store(processing_status);

-- ✅ Composite Indexes for Complex Queries
CREATE INDEX IF NOT EXISTS idx_event_composite_priority_status ON event_store(priority, processing_status);
CREATE INDEX IF NOT EXISTS idx_event_composite_source_type ON event_store(source_service, event_type);
CREATE INDEX IF NOT EXISTS idx_event_composite_status_timestamp ON event_store(processing_status, created_at);
CREATE INDEX IF NOT EXISTS idx_event_retry_status ON event_store(retry_count, processing_status);
CREATE INDEX IF NOT EXISTS idx_event_processing_time ON event_store(processing_start_time, processing_end_time);

-- ✅ Create WebSocket Connections table with comprehensive indexing
CREATE TABLE IF NOT EXISTS websocket_connections (
    connection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(100),
    client_ip VARCHAR(45),
    user_agent TEXT,
    connection_status VARCHAR(20) NOT NULL DEFAULT 'CONNECTED' 
        CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'IDLE', 'ERROR')),
    subscription_topics TEXT,
    messages_sent BIGINT NOT NULL DEFAULT 0 CHECK (messages_sent >= 0),
    messages_received BIGINT NOT NULL DEFAULT 0 CHECK (messages_received >= 0),
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    connection_duration_seconds BIGINT
);

-- ✅ WebSocket Connection Indexes for Production Performance
CREATE INDEX IF NOT EXISTS idx_ws_user_id ON websocket_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_ws_session_id ON websocket_connections(session_id);
CREATE INDEX IF NOT EXISTS idx_ws_status ON websocket_connections(connection_status);
CREATE INDEX IF NOT EXISTS idx_ws_created_at ON websocket_connections(created_at);
CREATE INDEX IF NOT EXISTS idx_ws_last_activity ON websocket_connections(last_activity_at);

-- ✅ Composite Indexes for Complex WebSocket Queries
CREATE INDEX IF NOT EXISTS idx_ws_composite_user_status ON websocket_connections(user_id, connection_status);
CREATE INDEX IF NOT EXISTS idx_ws_composite_status_activity ON websocket_connections(connection_status, last_activity_at);
CREATE INDEX IF NOT EXISTS idx_ws_active_connections ON websocket_connections(connection_status, created_at);
CREATE INDEX IF NOT EXISTS idx_ws_user_activity ON websocket_connections(user_id, last_activity_at);
CREATE INDEX IF NOT EXISTS idx_ws_performance_metrics ON websocket_connections(messages_sent, messages_received, connection_duration_seconds);

-- ✅ Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ✅ Apply trigger to event_store table
CREATE TRIGGER update_event_store_updated_at 
    BEFORE UPDATE ON event_store 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ✅ Create trigger to calculate connection duration
CREATE OR REPLACE FUNCTION calculate_connection_duration()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.connection_status = 'DISCONNECTED' AND OLD.connection_status != 'DISCONNECTED' THEN
        NEW.disconnected_at = CURRENT_TIMESTAMP;
        IF NEW.created_at IS NOT NULL THEN
            NEW.connection_duration_seconds = EXTRACT(EPOCH FROM (NEW.disconnected_at - NEW.created_at));
        END IF;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ✅ Apply trigger to websocket_connections table
CREATE TRIGGER calculate_ws_connection_duration 
    BEFORE UPDATE ON websocket_connections 
    FOR EACH ROW EXECUTE FUNCTION calculate_connection_duration();

-- ✅ Create views for common queries
CREATE OR REPLACE VIEW active_connections AS
SELECT 
    connection_id,
    session_id,
    user_id,
    client_ip,
    connection_status,
    last_activity_at,
    created_at,
    (messages_sent + messages_received) as total_messages
FROM websocket_connections 
WHERE connection_status = 'CONNECTED';

CREATE OR REPLACE VIEW pending_critical_events AS
SELECT 
    event_id,
    correlation_id,
    event_type,
    source_service,
    created_at,
    retry_count
FROM event_store 
WHERE processing_status = 'PENDING' 
  AND priority = 'CRITICAL'
ORDER BY created_at ASC;

-- ✅ Grant permissions for application user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON event_store TO event_bus_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON websocket_connections TO event_bus_app;
-- GRANT SELECT ON active_connections TO event_bus_app;
-- GRANT SELECT ON pending_critical_events TO event_bus_app;

-- ✅ Performance optimization settings
-- These would typically be set at database level
-- SET shared_preload_libraries = 'pg_stat_statements';
-- SET track_activity_query_size = 2048;
-- SET log_min_duration_statement = 100;
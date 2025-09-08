# TradeMaster Event Bus Service - Production Dockerfile
# Java 24 + Virtual Threads + Spring Boot 3.5.3 + Consul Integration

# Runtime stage - Java 24 Application Runtime
from amazoncorretto:24-alpine-jdk as runtime

# Metadata
label maintainer="TradeMaster Platform Team <platform@trademaster.com>" \
      version="1.0.0" \
      description="TradeMaster Event Bus Service with Java 24 Virtual Threads and Consul Integration" \
      java.version="24" \
      spring.boot.version="3.5.3" \
      consul.enabled="true"

# Install runtime dependencies
run apk update && apk add --no-cache \
    curl \
    jq \
    netcat-openbsd \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Create application user
run addgroup -g 1001 -S trademaster && \
    adduser -S trademaster -u 1001 -G trademaster

# Create application directories
run mkdir -p /app/logs /app/config /app/tmp && \
    chown -R trademaster:trademaster /app

# Set working directory
workdir /app

# Copy pre-built JAR from host (built with Java 24)
copy --chown=trademaster:trademaster build/libs/event-bus-service-*.jar app.jar

# Copy configuration files  
copy --chown=trademaster:trademaster src/main/resources/application*.yml config/

# Create entrypoint script with proper signal handling
run printf '#!/bin/sh\nset -e\n\n# Function to handle shutdown gracefully\nshutdown() {\n    echo "Received shutdown signal, gracefully shutting down..."\n    if [ -n "$APP_PID" ]; then\n        kill -TERM "$APP_PID" 2>/dev/null || true\n        wait "$APP_PID" 2>/dev/null || true\n    fi\n    exit 0\n}\n\n# Trap signals\ntrap shutdown TERM INT\n\n# Wait for dependencies if specified\nif [ -n "$WAIT_FOR_CONSUL" ]; then\n    echo "Waiting for Consul at ${CONSUL_HOST:-consul-server}:${CONSUL_PORT:-8500}..."\n    while ! nc -z "${CONSUL_HOST:-consul-server}" "${CONSUL_PORT:-8500}"; do\n        echo "Consul not ready, waiting..."\n        sleep 2\n    done\n    echo "Consul is ready!"\nfi\n\nif [ -n "$WAIT_FOR_POSTGRES" ]; then\n    echo "Waiting for PostgreSQL at ${DATABASE_HOST:-postgres-eventbus}..."\n    while ! nc -z "${DATABASE_HOST:-postgres-eventbus}" 5432; do\n        echo "PostgreSQL not ready, waiting..."\n        sleep 2\n    done\n    echo "PostgreSQL is ready!"\nfi\n\nif [ -n "$WAIT_FOR_REDIS" ]; then\n    echo "Waiting for Redis at ${REDIS_HOST:-redis-eventbus}..."\n    while ! nc -z "${REDIS_HOST:-redis-eventbus}" "${REDIS_PORT:-6379}"; do\n        echo "Redis not ready, waiting..."\n        sleep 2\n    done\n    echo "Redis is ready!"\nfi\n\n# Combine all JVM options\nFULL_JAVA_OPTS="$JVM_OPTS $MEMORY_OPTS $VIRTUAL_THREADS_OPTS $APP_OPTS $CONSUL_OPTS $SECURITY_OPTS $JAVA_OPTS"\n\necho "Starting Event Bus Service with Java options:"\necho "$FULL_JAVA_OPTS"\n\n# Start the application in background\njava $FULL_JAVA_OPTS -jar app.jar &\nAPP_PID=$!\n\n# Wait for the application to finish\nwait "$APP_PID"\n' > /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh && \
    chown trademaster:trademaster /app/entrypoint.sh

# Switch to non-root user
user trademaster

# Expose ports
expose 8081

# Environment variables
env SPRING_PROFILES_ACTIVE="docker,consul" \
    SERVER_PORT=8081 \
    LOGGING_LEVEL_ROOT=INFO \
    LOGGING_LEVEL_COM_TRADEMASTER=DEBUG

# JVM Configuration for Production with Java 24 Virtual Threads
env JVM_OPTS="--enable-preview \
-XX:+UseZGC \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseTransparentHugePages \
-XX:MaxRAMPercentage=75.0 \
-XX:MaxMetaspaceSize=256m \
-XX:MetaspaceSize=128m \
-XX:MaxDirectMemorySize=512m \
-XX:+UseCompressedOops \
-XX:+UseCompressedClassPointers \
-XX:+OptimizeStringConcat \
-XX:+UseStringDeduplication \
-Xlog:gc*:logs/gc.log:time,tags \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=logs/heapdump.hprof \
-Djava.security.egd=file:/dev/./urandom \
-Djava.awt.headless=true \
-Dfile.encoding=UTF-8 \
-Duser.timezone=UTC \
-Djava.net.preferIPv4Stack=true"

# Memory Configuration (defaults - can be overridden)
env MEMORY_OPTS="-Xms512m -Xmx2g"

# Virtual Threads Configuration
env VIRTUAL_THREADS_OPTS="-Djdk.virtualThreadScheduler.parallelism=200 \
-Djdk.virtualThreadScheduler.maxPoolSize=1000 \
-Dspring.threads.virtual.enabled=true"

# Application Configuration
env APP_OPTS="-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
-Dserver.port=${SERVER_PORT} \
-Dlogging.level.root=${LOGGING_LEVEL_ROOT} \
-Dlogging.level.com.trademaster=${LOGGING_LEVEL_COM_TRADEMASTER} \
-Dmanagement.endpoints.web.exposure.include=health,metrics,info,prometheus,circuitbreakers,consul \
-Dmanagement.endpoint.health.show-details=always \
-Dmanagement.endpoint.health.probes.enabled=true"

# Consul Configuration
env CONSUL_OPTS="-Dspring.cloud.consul.enabled=true \
-Dspring.cloud.consul.discovery.enabled=true \
-Dspring.cloud.consul.discovery.prefer-ip-address=true \
-Dspring.cloud.consul.discovery.health-check-path=/actuator/health \
-Dspring.cloud.consul.discovery.health-check-interval=10s \
-Dspring.cloud.consul.discovery.metadata.health_path=/actuator/health \
-Dspring.cloud.consul.discovery.metadata.metrics_path=/actuator/metrics \
-Dspring.cloud.consul.discovery.metadata.info_path=/actuator/info \
-Dspring.cloud.consul.discovery.metadata.prometheus_path=/actuator/prometheus \
-Dspring.cloud.consul.discovery.metadata.prometheus_scrape=true \
-Dspring.cloud.consul.discovery.metadata.prometheus_port=9081 \
-Dspring.cloud.consul.discovery.metadata.sla_critical=25ms \
-Dspring.cloud.consul.discovery.metadata.sla_high=50ms \
-Dspring.cloud.consul.discovery.metadata.datacenter=trademaster-dc \
-Dspring.cloud.consul.discovery.metadata.service_mesh=consul-connect-ready \
-Dspring.cloud.consul.discovery.metadata.management_context_path=/actuator \
-Dspring.cloud.consul.config.enabled=true \
-Dspring.cloud.consul.config.format=YAML \
-Dspring.cloud.consul.config.watch.enabled=true"

# Security Configuration
env SECURITY_OPTS="-Dnetworkaddress.cache.ttl=60 \
-Dnetworkaddress.cache.negative.ttl=10"

# Health check
healthcheck --interval=30s --timeout=10s --start-period=60s --retries=3 \
    cmd curl -f http://localhost:8081/actuator/health || exit 1

# Use dumb-init for proper signal handling
entrypoint ["/usr/bin/dumb-init", "--"]
cmd ["/app/entrypoint.sh"]
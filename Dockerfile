# ✅ PRODUCTION DOCKERFILE: Event Bus Service
# Java 24 + Virtual Threads + Spring Boot 3.5.3
# Multi-stage build for optimized production container

FROM eclipse-temurin:24-jdk-alpine AS builder

# ✅ VIRTUAL THREADS: Set JVM flags for Java 24 preview features
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:+UnlockExperimentalVMOptions"

# ✅ SECURITY: Create non-root user for build
RUN addgroup -g 1001 -S eventbus && \
    adduser -S eventbus -u 1001 -G eventbus

# ✅ PERFORMANCE: Set working directory and copy files
WORKDIR /app
COPY --chown=eventbus:eventbus . .

# ✅ PRODUCTION: Build the application with Gradle
RUN chmod +x ./gradlew && \
    ./gradlew bootJar --no-daemon && \
    java -Djarmode=layertools -jar build/libs/*.jar extract

# ✅ PRODUCTION RUNTIME IMAGE
FROM eclipse-temurin:24-jre-alpine AS runtime

# ✅ SECURITY: Install security updates and create user
RUN apk update && apk upgrade && \
    addgroup -g 1001 -S eventbus && \
    adduser -S eventbus -u 1001 -G eventbus

# ✅ PERFORMANCE: Set JVM optimization flags
ENV JAVA_OPTS="--enable-preview \
    -XX:+UseZGC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -server"

# ✅ MONITORING: Add application performance monitoring
ENV SPRING_PROFILES_ACTIVE=docker
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,info,prometheus
ENV MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always

# ✅ SECURITY: Set working directory and user
WORKDIR /app
USER eventbus:eventbus

# ✅ LAYERED ARCHITECTURE: Copy layered JAR for better caching
COPY --from=builder --chown=eventbus:eventbus /app/dependencies/ ./
COPY --from=builder --chown=eventbus:eventbus /app/spring-boot-loader/ ./
COPY --from=builder --chown=eventbus:eventbus /app/snapshot-dependencies/ ./
COPY --from=builder --chown=eventbus:eventbus /app/application/ ./

# ✅ HEALTH CHECK: Container health monitoring
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8099/event-bus/api/v1/health/live || exit 1

# ✅ NETWORK: Expose ports for service and management
EXPOSE 8099 8090

# ✅ PRODUCTION: Start application with optimized JVM
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# ✅ METADATA: Container labels for production
LABEL maintainer="TradeMaster Platform Team" \
      service.name="event-bus-service" \
      service.version="1.0.0" \
      java.version="24" \
      spring.boot.version="3.5.3" \
      build.architecture="multi-stage" \
      security.user="eventbus" \
      performance.jvm="ZGC" \
      monitoring.enabled="true"
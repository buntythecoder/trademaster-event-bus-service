# 🚀 TradeMaster Event Bus Service - Deployment Guide

**Version**: 1.0.0  
**Target Environments**: Development, Staging, Production  
**Platform**: Kubernetes + Docker  
**Last Updated**: September 6, 2025

## 📋 Prerequisites

### System Requirements
- **Kubernetes**: v1.28+
- **Docker**: v20.10+
- **Java Runtime**: Java 24 with `--enable-preview`
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Message Queue**: Apache Kafka 3.9+

### Access Requirements
- Kubernetes cluster admin access
- Docker registry push/pull permissions
- Database admin credentials
- Monitoring system access (Prometheus, Grafana)

### Pre-Deployment Validation
```bash
# Verify Java 24 support
java --version --enable-preview

# Check Kubernetes connectivity
kubectl cluster-info

# Verify registry access
docker login ghcr.io

# Test database connectivity
psql -h postgres-cluster -p 5432 -U postgres
```

## 🏗️ Build and Package

### 1. Application Build
```bash
# Navigate to project directory
cd event-bus-service

# Clean and build
./gradlew clean bootJar --no-daemon

# Verify build artifact
ls -la build/libs/event-bus-service-1.0.0.jar
```

### 2. Docker Image Build
```bash
# Build Docker image
docker build -t ghcr.io/trademaster/event-bus-service:1.0.0 .

# Tag as latest
docker tag ghcr.io/trademaster/event-bus-service:1.0.0 ghcr.io/trademaster/event-bus-service:latest

# Push to registry
docker push ghcr.io/trademaster/event-bus-service:1.0.0
docker push ghcr.io/trademaster/event-bus-service:latest
```

### 3. Build Verification
```bash
# Test image locally
docker run --rm -p 8080:8080 ghcr.io/trademaster/event-bus-service:1.0.0

# Health check
curl -s http://localhost:8080/actuator/health
```

## 🌍 Environment-Specific Deployments

### Development Environment

#### Configuration
```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/eventbus_dev
    username: dev_user
  redis:
    host: localhost
    port: 6379
    database: 0
logging:
  level:
    com.trademaster: DEBUG
```

#### Deployment Commands
```bash
# Create development namespace
kubectl create namespace event-bus-development

# Apply development configuration
kubectl apply -f k8s/development/

# Verify deployment
kubectl get pods -n event-bus-development
kubectl get services -n event-bus-development

# Port forward for local access
kubectl port-forward -n event-bus-development service/event-bus-service 8080:80
```

### Staging Environment

#### Configuration
```yaml
# application-staging.yml
spring:
  profiles:
    active: staging
  datasource:
    url: jdbc:postgresql://postgres-staging:5432/eventbus_staging
    username: staging_user
  redis:
    host: redis-staging
    port: 6379
logging:
  level:
    com.trademaster: INFO
```

#### Deployment Commands
```bash
# Create staging namespace
kubectl create namespace event-bus-staging

# Create secrets
kubectl create secret generic event-bus-service-secrets \
  --from-literal=db-username=staging_user \
  --from-literal=db-password=staging_pass \
  --from-literal=redis-password=staging_redis \
  --from-literal=jwt-secret=staging_jwt_secret \
  -n event-bus-staging

# Apply staging configuration
kubectl apply -f k8s/staging/

# Verify deployment
kubectl get all -n event-bus-staging

# Run smoke tests
kubectl run -i --tty --rm test --image=curlimages/curl --restart=Never -- \
  curl -s http://event-bus-service.event-bus-staging:80/actuator/health
```

### Production Environment

#### Pre-Production Checklist
- [ ] All tests pass in staging
- [ ] Performance benchmarks met
- [ ] Security scan completed
- [ ] Database migrations tested
- [ ] Monitoring alerts configured
- [ ] Rollback plan prepared

#### Production Secrets Management
```bash
# Create production secrets (use proper secret management)
kubectl create secret generic event-bus-service-secrets \
  --from-literal=db-username=${PROD_DB_USER} \
  --from-literal=db-password=${PROD_DB_PASS} \
  --from-literal=redis-password=${PROD_REDIS_PASS} \
  --from-literal=jwt-secret=${PROD_JWT_SECRET} \
  --from-literal=kafka-username=${PROD_KAFKA_USER} \
  --from-literal=kafka-password=${PROD_KAFKA_PASS} \
  -n event-bus-production
```

#### Blue-Green Deployment
```bash
# Step 1: Deploy green version
kubectl apply -f k8s/production/

# Step 2: Verify green deployment
kubectl get pods -n event-bus-production -l version=1.0.0

# Step 3: Health check
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  curl -s localhost:8081/actuator/health

# Step 4: Update service to point to green
kubectl patch service event-bus-service -n event-bus-production \
  -p '{"spec":{"selector":{"version":"1.0.0"}}}'

# Step 5: Monitor for issues
kubectl get events -n event-bus-production --sort-by=.metadata.creationTimestamp

# Step 6: Scale down blue version (after verification)
kubectl scale deployment event-bus-service-blue --replicas=0 -n event-bus-production
```

#### Canary Deployment (Alternative)
```bash
# Step 1: Deploy canary with 10% traffic
kubectl apply -f k8s/production/canary/

# Step 2: Monitor canary metrics
kubectl logs -f -n event-bus-production deployment/event-bus-service-canary

# Step 3: Gradually increase traffic
kubectl patch deployment event-bus-service-canary -n event-bus-production \
  -p '{"spec":{"replicas":2}}'

# Step 4: Full rollout after validation
kubectl patch service event-bus-service -n event-bus-production \
  -p '{"spec":{"selector":{"version":"1.0.0"}}}'
```

## 🔧 Configuration Management

### ConfigMap Management
```bash
# Create ConfigMap from file
kubectl create configmap event-bus-service-config \
  --from-file=application.yml \
  --from-file=application-prod.yml \
  -n event-bus-production

# Update existing ConfigMap
kubectl create configmap event-bus-service-config \
  --from-file=application.yml \
  --dry-run=client -o yaml | kubectl apply -f -

# Verify ConfigMap content
kubectl get configmap event-bus-service-config -o yaml -n event-bus-production
```

### Environment-Specific Overrides
```yaml
# production-overrides.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: event-bus-service-config
data:
  application-prod.yml: |
    spring:
      profiles:
        active: production
      datasource:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
    trademaster:
      security:
        rate-limit:
          requests-per-minute: 10000
      performance:
        sla:
          critical-ms: 25
          high-ms: 50
```

## 📊 Monitoring and Observability

### Prometheus Configuration
```yaml
# prometheus-config.yml
- job_name: 'event-bus-service'
  kubernetes_sd_configs:
  - role: endpoints
    namespaces:
      names:
      - event-bus-production
  relabel_configs:
  - source_labels: [__meta_kubernetes_service_name]
    action: keep
    regex: event-bus-service-management
```

### Grafana Dashboard Import
```bash
# Import pre-configured dashboard
curl -X POST http://grafana:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @monitoring/grafana-dashboard.json
```

### Alert Manager Configuration
```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'event-bus-alerts'

receivers:
- name: 'event-bus-alerts'
  slack_configs:
  - api_url: '${SLACK_WEBHOOK_URL}'
    channel: '#alerts-production'
    title: 'Event Bus Service Alert'
    text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
```

## 🔄 Database Migrations

### Liquibase Integration
```bash
# Run migrations manually (if needed)
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  java -jar app.jar --liquibase.change-log=classpath:db/changelog/db.changelog-master.yml

# Verify migration status
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  java -jar app.jar --liquibase.status
```

### Migration Rollback
```bash
# Rollback last migration
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  java -jar app.jar --liquibase.rollback-count=1

# Rollback to specific tag
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  java -jar app.jar --liquibase.rollback=v1.0.0
```

## 🧪 Smoke Testing

### Post-Deployment Validation
```bash
#!/bin/bash
# smoke-tests.sh

BASE_URL="http://event-bus-service.event-bus-production"

# Health check
echo "Testing health endpoint..."
curl -f "${BASE_URL}/actuator/health" || exit 1

# API endpoints
echo "Testing API endpoints..."
curl -f "${BASE_URL}/api/v1/event-bus/agents" || exit 1

# Performance metrics
echo "Checking performance metrics..."
curl -s "${BASE_URL}/api/v1/event-bus/performance/stats" | jq '.complianceRate' | grep -v null || exit 1

# Circuit breaker status
echo "Verifying circuit breakers..."
curl -s "${BASE_URL}/actuator/circuitbreakers" | jq '.[] | select(.state != "CLOSED")' | grep -q . && exit 1

echo "All smoke tests passed!"
```

### Load Testing
```bash
# Simple load test
kubectl run load-test --image=jordi/ab --rm -it --restart=Never -- \
  ab -n 1000 -c 10 http://event-bus-service.event-bus-production/actuator/health

# K6 load test
kubectl run k6-test --image=loadimpact/k6 --rm -it --restart=Never -- \
  run --vus 10 --duration 30s - <<EOF
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  let response = http.get('http://event-bus-service.event-bus-production/actuator/health');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });
}
EOF
```

## 🔙 Rollback Procedures

### Immediate Rollback
```bash
# Quick rollback to previous version
kubectl rollout undo deployment/event-bus-service -n event-bus-production

# Monitor rollback status
kubectl rollout status deployment/event-bus-service -n event-bus-production

# Verify service recovery
curl -s http://event-bus-service.event-bus-production/actuator/health
```

### Rollback to Specific Version
```bash
# List deployment history
kubectl rollout history deployment/event-bus-service -n event-bus-production

# Rollback to specific revision
kubectl rollout undo deployment/event-bus-service --to-revision=2 -n event-bus-production

# Scale to previous configuration
kubectl scale deployment event-bus-service --replicas=3 -n event-bus-production
```

### Database Rollback
```bash
# If database changes need to be rolled back
kubectl exec -n event-bus-production deployment/event-bus-service -- \
  java -jar app.jar --liquibase.rollback-to-date=2025-09-06

# Verify data integrity after rollback
kubectl exec -n database postgres-cluster-0 -- \
  psql -U eventbus_user eventbus_db -c "SELECT COUNT(*) FROM agents;"
```

## ⚠️ Troubleshooting

### Common Deployment Issues

#### Pod Stuck in Pending
```bash
# Check node resources
kubectl describe nodes

# Check resource limits
kubectl describe pod <pod-name> -n event-bus-production

# Check persistent volume claims
kubectl get pvc -n event-bus-production
```

#### Image Pull Errors
```bash
# Verify image exists
docker pull ghcr.io/trademaster/event-bus-service:1.0.0

# Check image pull secrets
kubectl get secrets -n event-bus-production | grep docker

# Create image pull secret if missing
kubectl create secret docker-registry github-registry-secret \
  --docker-server=ghcr.io \
  --docker-username=$GITHUB_USERNAME \
  --docker-password=$GITHUB_TOKEN \
  -n event-bus-production
```

#### Service Discovery Issues
```bash
# Check service endpoints
kubectl get endpoints event-bus-service -n event-bus-production

# Verify pod labels match service selector
kubectl get pods -n event-bus-production --show-labels
kubectl describe service event-bus-service -n event-bus-production
```

### Recovery Procedures

#### Complete Service Recovery
```bash
# 1. Stop all pods
kubectl scale deployment event-bus-service --replicas=0 -n event-bus-production

# 2. Clear persistent volumes if corrupted
kubectl delete pvc data-volume -n event-bus-production

# 3. Recreate from clean state
kubectl apply -f k8s/production/

# 4. Monitor recovery
kubectl get pods -n event-bus-production -w
```

#### Configuration Recovery
```bash
# Restore from backup configuration
kubectl create configmap event-bus-service-config \
  --from-file=backup/application.yml \
  --from-file=backup/application-prod.yml \
  -n event-bus-production

# Restart pods to pick up configuration
kubectl rollout restart deployment/event-bus-service -n event-bus-production
```

---

**Document Owner**: DevOps Engineering Team  
**Review Schedule**: Quarterly  
**Next Review**: December 6, 2025
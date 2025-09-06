# 🚀 TradeMaster Event Bus Service - Operations Runbook

**Service**: Event Bus Service v1.0.0  
**Technology**: Java 24 + Spring Boot 3.5.3 + Virtual Threads  
**Environment**: Production  
**Documentation Version**: 1.0  
**Last Updated**: September 6, 2025

## 📋 Quick Reference

### Service Endpoints
- **Application**: `http://localhost:8080`
- **Health Check**: `http://localhost:8081/actuator/health`
- **Metrics**: `http://localhost:8081/actuator/prometheus`
- **API Documentation**: `http://localhost:8080/swagger-ui/index.html`

### Key Metrics to Monitor
- **SLA Compliance Rate**: Target >95%
- **Response Time**: <200ms for standard operations
- **Memory Usage**: <2GB heap
- **Circuit Breaker Status**: All breakers should be CLOSED
- **Virtual Thread Count**: Monitor for thread exhaustion

## 🚨 Incident Response Procedures

### Severity Levels

#### **P0 - Critical (Service Down)**
- **Response Time**: 15 minutes
- **Escalation**: Immediate
- **Examples**: Service won't start, all endpoints returning 5xx, database connectivity lost

#### **P1 - High (Degraded Performance)**
- **Response Time**: 1 hour
- **Escalation**: 30 minutes
- **Examples**: SLA compliance <90%, circuit breakers open, memory leaks

#### **P2 - Medium (Minor Issues)**
- **Response Time**: 4 hours
- **Escalation**: 2 hours
- **Examples**: Individual feature failures, performance warnings

#### **P3 - Low (Maintenance)**
- **Response Time**: Next business day
- **Escalation**: None
- **Examples**: Documentation updates, minor configuration changes

## 🔧 Common Troubleshooting Scenarios

### 1. Service Won't Start

#### Symptoms
- Application fails to start
- Port binding errors
- Database connection errors

#### Diagnostic Steps
```bash
# Check application logs
tail -f /opt/trademaster/logs/application.log

# Verify Java version
java --version

# Check port availability
netstat -tlnp | grep 8080

# Verify database connectivity
psql -h postgres-cluster -p 5432 -U eventbus_user -d eventbus_db
```

#### Resolution Steps
1. Check Java 24 with `--enable-preview` flag
2. Verify database connectivity and credentials
3. Ensure ports 8080/8081 are available
4. Check configuration file syntax
5. Review startup logs for specific errors

### 2. High Memory Usage

#### Symptoms
- Memory usage >80%
- OutOfMemoryError in logs
- Slow response times

#### Diagnostic Commands
```bash
# Check memory usage
ps aux | grep java
cat /proc/$(pgrep java)/status | grep VmRSS

# Generate heap dump
jcmd $(pgrep java) GC.run_finalization
jcmd $(pgrep java) VM.memory

# Monitor GC activity
jstat -gc $(pgrep java) 5s
```

#### Resolution Steps
1. Trigger manual garbage collection
2. Analyze heap dump for memory leaks
3. Check for memory-intensive operations
4. Scale horizontally if needed
5. Adjust JVM memory settings if persistent

### 3. Circuit Breakers Open

#### Symptoms
- Circuit breakers in OPEN state
- Increased error rates
- Fallback responses being returned

#### Diagnostic Commands
```bash
# Check circuit breaker status
curl -s http://localhost:8081/actuator/circuitbreakers | jq

# Force reset circuit breaker
curl -X POST http://localhost:8080/api/v1/event-bus/circuit-breaker/database/reset
```

#### Resolution Steps
1. Identify which service is causing failures
2. Check downstream service health
3. Review error logs for root cause
4. Fix underlying issue before resetting breakers
5. Monitor recovery after reset

### 4. SLA Compliance Issues

#### Symptoms
- SLA compliance rate <95%
- Slow response times
- Performance degradation

#### Diagnostic Commands
```bash
# Check performance statistics
curl -s http://localhost:8081/api/v1/event-bus/performance/stats

# Monitor real-time performance
watch -n 5 'curl -s http://localhost:8081/actuator/metrics/http.server.requests | jq'

# Check database performance
psql -c "SELECT query, mean_exec_time, calls FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

#### Resolution Steps
1. Identify performance bottlenecks
2. Check database query performance
3. Review virtual thread utilization
4. Consider scaling resources
5. Optimize slow operations

### 5. Database Connectivity Issues

#### Symptoms
- Database operation failures
- Connection timeout errors
- Database circuit breaker open

#### Diagnostic Commands
```bash
# Test database connection
pg_isready -h postgres-cluster -p 5432

# Check connection pool status
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections

# Monitor database performance
psql -c "SELECT state, count(*) FROM pg_stat_activity GROUP BY state;"
```

#### Resolution Steps
1. Verify database server is running
2. Check network connectivity
3. Validate credentials and permissions
4. Review connection pool configuration
5. Check for long-running transactions

## 📊 Monitoring and Alerting

### Key Metrics to Track

#### Application Metrics
```yaml
# SLA Compliance
- Metric: custom_sla_compliance_rate
- Threshold: < 95%
- Severity: P1

# Response Time
- Metric: http_request_duration_seconds_p95
- Threshold: > 0.2s
- Severity: P2

# Error Rate
- Metric: http_requests_total{status=~"5.."}
- Threshold: > 1%
- Severity: P1
```

#### Infrastructure Metrics
```yaml
# Memory Usage
- Metric: jvm_memory_used_bytes / jvm_memory_max_bytes
- Threshold: > 80%
- Severity: P1

# CPU Usage
- Metric: system_cpu_usage
- Threshold: > 80%
- Severity: P2

# Virtual Thread Count
- Metric: jvm_threads_live_threads
- Threshold: > 1000
- Severity: P2
```

#### Business Metrics
```yaml
# Event Processing Rate
- Metric: events_processed_total
- Threshold: < 1000/min
- Severity: P2

# Circuit Breaker Status
- Metric: resilience4j_circuitbreaker_state
- Threshold: != CLOSED
- Severity: P1
```

### Alert Configuration

#### Prometheus Alert Rules
```yaml
groups:
  - name: event-bus-service
    rules:
      - alert: EventBusServiceDown
        expr: up{job="event-bus-service"} == 0
        for: 1m
        labels:
          severity: P0
        annotations:
          summary: "Event Bus Service is down"
          
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: P1
        annotations:
          summary: "High memory usage detected"
          
      - alert: SLAComplianceIssue
        expr: sla_compliance_rate < 95
        for: 10m
        labels:
          severity: P1
        annotations:
          summary: "SLA compliance below threshold"
```

## 🔄 Deployment Procedures

### Rolling Deployment

#### Pre-Deployment Checklist
- [ ] Verify new version builds successfully
- [ ] Run full test suite
- [ ] Check compatibility with dependencies
- [ ] Review configuration changes
- [ ] Backup current configuration

#### Deployment Steps
```bash
# 1. Deploy to staging
kubectl apply -f k8s/staging/

# 2. Run smoke tests
curl -s http://staging.event-bus/actuator/health

# 3. Deploy to production (blue-green)
kubectl apply -f k8s/production/

# 4. Monitor deployment
kubectl get pods -n event-bus-production -w

# 5. Verify health
kubectl exec -n event-bus-production deployment/event-bus-service -- curl localhost:8081/actuator/health
```

#### Post-Deployment Verification
- [ ] All pods running successfully
- [ ] Health checks passing
- [ ] SLA compliance maintained
- [ ] No error rate increase
- [ ] Circuit breakers closed

#### Rollback Procedure
```bash
# If issues detected, immediate rollback
kubectl rollout undo deployment/event-bus-service -n event-bus-production

# Monitor rollback
kubectl rollout status deployment/event-bus-service -n event-bus-production

# Verify service recovery
curl -s http://production.event-bus/actuator/health
```

### Configuration Management

#### Configuration Files
- **application.yml**: Main application configuration
- **application-prod.yml**: Production overrides
- **k8s/production/configmap.yaml**: Kubernetes configuration
- **k8s/production/secrets.yaml**: Sensitive configuration

#### Configuration Changes
```bash
# Update ConfigMap
kubectl create configmap event-bus-service-config --from-file=application.yml --dry-run=client -o yaml | kubectl apply -f -

# Restart pods to pick up new configuration
kubectl rollout restart deployment/event-bus-service -n event-bus-production

# Verify configuration applied
kubectl exec -n event-bus-production deployment/event-bus-service -- cat /opt/trademaster/config/application.yml
```

## 💾 Backup and Recovery

### Database Backups

#### Automated Backup
```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h postgres-cluster -U eventbus_user eventbus_db > /backups/eventbus_${DATE}.sql
gzip /backups/eventbus_${DATE}.sql

# Retention: Keep 30 days
find /backups -name "eventbus_*.sql.gz" -mtime +30 -delete
```

#### Manual Backup
```bash
# Create immediate backup
kubectl exec -n database postgres-cluster-0 -- pg_dump -U eventbus_user eventbus_db | gzip > eventbus_backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

#### Recovery Procedure
```bash
# 1. Stop application
kubectl scale deployment event-bus-service --replicas=0 -n event-bus-production

# 2. Restore database
gunzip -c eventbus_backup_20250906_120000.sql.gz | kubectl exec -i -n database postgres-cluster-0 -- psql -U eventbus_user eventbus_db

# 3. Restart application
kubectl scale deployment event-bus-service --replicas=3 -n event-bus-production

# 4. Verify recovery
curl -s http://production.event-bus/actuator/health
```

## 🔍 Log Analysis

### Log Locations
- **Application Logs**: `/opt/trademaster/logs/application.log`
- **Access Logs**: `/opt/trademaster/logs/access.log`
- **Error Logs**: `/opt/trademaster/logs/error.log`
- **Kubernetes Logs**: `kubectl logs -n event-bus-production deployment/event-bus-service`

### Common Log Patterns
```bash
# Error patterns to watch for
grep -E "(ERROR|FATAL|OutOfMemoryError|SQLException)" /opt/trademaster/logs/application.log

# Performance issues
grep -E "(SLA VIOLATION|Circuit breaker)" /opt/trademaster/logs/application.log

# Security concerns
grep -E "(WARN.*Security|Authentication failed|Unauthorized)" /opt/trademaster/logs/application.log
```

### Centralized Logging
```bash
# View logs in Kubernetes
kubectl logs -f -n event-bus-production deployment/event-bus-service

# Filter by log level
kubectl logs -n event-bus-production deployment/event-bus-service | grep ERROR

# View logs from specific time
kubectl logs --since=1h -n event-bus-production deployment/event-bus-service
```

## 📞 Contact Information

### Escalation Matrix
- **L1 Support**: operations@trademaster.com
- **L2 Engineering**: engineering@trademaster.com  
- **L3 Architecture**: architecture@trademaster.com
- **Emergency**: +1-555-0199 (24/7)

### Team Responsibilities
- **Platform Team**: Infrastructure, deployment, monitoring
- **Development Team**: Application bugs, feature issues
- **Security Team**: Security incidents, compliance
- **Database Team**: Database performance, backup/recovery

---

**Document Owner**: Platform Engineering Team  
**Review Schedule**: Monthly  
**Next Review**: October 6, 2025
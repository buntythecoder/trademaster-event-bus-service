package com.trademaster.eventbus.security;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Single Responsibility: Security Risk Assessment
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: SOLID Principles - Single Responsibility for risk assessment
 * - Rule #3: Functional Programming - No if-else, pattern matching
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total
 * - Rule #12: Virtual Threads for all async operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityRiskAssessmentService {
    
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    // ✅ IMMUTABLE: Risk tracking state
    private final ConcurrentHashMap<String, UserRiskProfile> userRiskProfiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastRequestTimes = new ConcurrentHashMap<>();
    
    // ✅ CONSTANTS: Risk assessment thresholds
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int SUSPICIOUS_REQUEST_THRESHOLD = 100;
    
    /**
     * ✅ FUNCTIONAL: Assess security risk for user operation
     */
    public CompletableFuture<Result<RiskAssessment, GatewayError>> assessRisk(
            String userId, Map<String, String> context) {
        
        return CompletableFuture.supplyAsync(() -> 
            calculateRiskScore(userId, context)
                .fold(
                    riskScore -> Result.success(new RiskAssessment(
                        determineRiskLevel(riskScore),
                        generateRiskAssessment(riskScore, context),
                        Map.of(
                            "risk_score", riskScore.toString(),
                            "user_id", userId,
                            "assessment_time", Instant.now().toString(),
                            "context", context.toString()
                        )
                    )),
                    error -> Result.<RiskAssessment, GatewayError>failure(error)
                ), virtualThreadExecutor);
    }
    
    // ✅ PRIVATE HELPERS: Functional risk calculation
    
    private Result<Integer, GatewayError> calculateRiskScore(String userId, Map<String, String> context) {
        return java.util.Optional.ofNullable(userId)
            .map(id -> calculateBaseRiskScore(id)
                + calculateRateLimitRisk(id)
                + calculateContextRisk(context)
                + calculateUserBehaviorRisk(id))
            .filter(score -> score >= 0)
            .map(Result::<Integer, GatewayError>success)
            .orElse(Result.failure(new GatewayError.SystemError.InternalServerError(
                "Failed to calculate risk score", "RISK_CALCULATION_ERROR")));
    }
    
    private int calculateBaseRiskScore(String userId) {
        return switch (getUserRiskCategory(userId)) {
            case TRUSTED -> 0;
            case NORMAL -> 10;
            case SUSPICIOUS -> 50;
            case BLOCKED -> 100;
        };
    }
    
    private int calculateRateLimitRisk(String userId) {
        Instant now = Instant.now();
        Instant lastRequest = lastRequestTimes.put(userId, now);
        
        return java.util.Optional.ofNullable(lastRequest)
            .filter(last -> Duration.between(last, now).compareTo(RATE_LIMIT_WINDOW) < 0)
            .map(_ -> {
                UserRiskProfile profile = userRiskProfiles.computeIfAbsent(userId, 
                    id -> new UserRiskProfile(id, 0, Instant.now()));
                int newRequestCount = profile.requestCount() + 1;
                userRiskProfiles.put(userId, profile.withRequestCount(newRequestCount));
                return calculateRateLimitPenalty(newRequestCount);
            })
            .orElse(0);
    }
    
    private int calculateRateLimitPenalty(int requestCount) {
        return switch (requestCount) {
            case int count when count > SUSPICIOUS_REQUEST_THRESHOLD -> 80;
            case int count when count > MAX_REQUESTS_PER_MINUTE -> 40;
            case int count when count > MAX_REQUESTS_PER_MINUTE / 2 -> 20;
            default -> 0;
        };
    }
    
    private int calculateContextRisk(Map<String, String> context) {
        return context.entrySet().stream()
            .mapToInt(entry -> switch (entry.getKey()) {
                case "ip_address" -> assessIpRisk(entry.getValue());
                case "user_agent" -> assessUserAgentRisk(entry.getValue());
                default -> 0;
            })
            .sum();
    }
    
    private int assessIpRisk(String ipAddress) {
        return switch (ipAddress) {
            case String ip when ip.startsWith("192.168.") -> 0; // Internal network
            case String ip when ip.startsWith("10.") -> 0; // Internal network
            case String ip when ip.equals("127.0.0.1") -> 0; // Localhost
            case null, default -> 10; // External/unknown
        };
    }
    
    private int assessUserAgentRisk(String userAgent) {
        return switch (userAgent) {
            case String ua when ua.contains("bot") || ua.contains("crawler") -> 30;
            case "unknown" -> 20;
            case null, default -> 0;
        };
    }
    
    private int calculateUserBehaviorRisk(String userId) {
        return java.util.Optional.ofNullable(userRiskProfiles.get(userId))
            .map(profile -> switch (profile.riskCategory()) {
                case TRUSTED -> -10; // Bonus for trusted users
                case NORMAL -> 0;
                case SUSPICIOUS -> 30;
                case BLOCKED -> 100;
            })
            .orElse(5); // Default for new users
    }
    
    private RiskLevel determineRiskLevel(int riskScore) {
        return switch (riskScore) {
            case int score when score >= 80 -> RiskLevel.CRITICAL;
            case int score when score >= 50 -> RiskLevel.HIGH;
            case int score when score >= 20 -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
    
    private String generateRiskAssessment(int riskScore, Map<String, String> context) {
        return switch (determineRiskLevel(riskScore)) {
            case CRITICAL -> "Critical risk detected - immediate intervention required";
            case HIGH -> "High risk detected - enhanced monitoring required";
            case MEDIUM -> "Medium risk detected - additional verification recommended";
            case LOW -> "Low risk - normal processing approved";
        };
    }
    
    private UserRiskCategory getUserRiskCategory(String userId) {
        return java.util.Optional.ofNullable(userRiskProfiles.get(userId))
            .map(UserRiskProfile::riskCategory)
            .orElse(UserRiskCategory.NORMAL);
    }
    
    // ✅ ENUMS: Type-safe risk classification
    
    private enum UserRiskCategory {
        TRUSTED, NORMAL, SUSPICIOUS, BLOCKED
    }
    
    public enum RiskLevel {
        LOW(true), MEDIUM(true), HIGH(false), CRITICAL(false);
        
        private final boolean acceptable;
        
        RiskLevel(boolean acceptable) {
            this.acceptable = acceptable;
        }
        
        public boolean isAcceptable() {
            return acceptable;
        }
    }
    
    // ✅ IMMUTABLE: Risk assessment records
    
    public record RiskAssessment(
        RiskLevel riskLevel,
        String assessment,
        Map<String, Object> riskFactors
    ) {}
    
    private record UserRiskProfile(
        String userId,
        int requestCount,
        Instant profileCreatedTime,
        UserRiskCategory riskCategory
    ) {
        public UserRiskProfile(String userId, int requestCount, Instant profileCreatedTime) {
            this(userId, requestCount, profileCreatedTime, UserRiskCategory.NORMAL);
        }
        
        public UserRiskProfile withRequestCount(int newRequestCount) {
            return new UserRiskProfile(userId, newRequestCount, profileCreatedTime, riskCategory);
        }
    }
}
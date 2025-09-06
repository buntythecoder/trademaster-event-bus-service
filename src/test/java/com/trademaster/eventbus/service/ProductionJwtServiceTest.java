package com.trademaster.eventbus.service;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import com.trademaster.eventbus.service.ProductionJwtService.JwtValidationResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ UNIT TESTS: ProductionJwtService Test Suite
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #20: >80% unit test coverage with functional test builders
 * - Real JWT token validation with jjwt library
 * - Functional programming test patterns with Result types
 * - Virtual Threads testing with async CompletableFuture patterns
 * 
 * TEST COVERAGE:
 * - JWT token generation and validation
 * - Token expiration and signature verification
 * - Blacklist functionality and security measures
 * - User context extraction and claims processing
 * - Error handling with functional Result patterns
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per test class
 */
class ProductionJwtServiceTest {

    private ProductionJwtService jwtService;
    private final String testSecret = "test-secret-key-for-jwt-signing-must-be-long-enough";
    private final String testIssuer = "test-event-bus";
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new ProductionJwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtIssuer", testIssuer);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", java.time.Duration.ofMinutes(30));
        
        signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
    }

    /**
     * ✅ TEST: Valid JWT token validation
     */
    @Test
    void shouldValidateValidJwtTokenSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Valid JWT token
        String validToken = createValidTestToken("user-123", List.of("TRADER", "VIEWER"));

        // ✅ WHEN: Validating token
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(validToken);

        // ✅ THEN: Validation succeeds with correct user context
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        JwtValidationResult validationResult = result.getValue().get();
        assertTrue(validationResult.valid());
        assertEquals("user-123", validationResult.userId());
        assertEquals(List.of("TRADER", "VIEWER"), validationResult.roles());
        assertNotNull(validationResult.tokenId());
        assertNotNull(validationResult.issuedAt());
        assertNotNull(validationResult.expiresAt());
    }

    /**
     * ✅ TEST: Null token validation
     */
    @Test
    void shouldFailValidationWithNullToken() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Null token
        String nullToken = null;

        // ✅ WHEN: Validating null token
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(nullToken);

        // ✅ THEN: Validation fails with invalid token error
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.InvalidToken);
        assertEquals("AUTH_001_INVALID_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Empty token validation
     */
    @Test
    void shouldFailValidationWithEmptyToken() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Empty token
        String emptyToken = "";

        // ✅ WHEN: Validating empty token
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(emptyToken);

        // ✅ THEN: Validation fails with invalid token error
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.InvalidToken);
        assertEquals("AUTH_001_INVALID_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Malformed token validation
     */
    @Test
    void shouldFailValidationWithMalformedToken() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Malformed token (not 3 parts)
        String malformedToken = "invalid.token";

        // ✅ WHEN: Validating malformed token
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(malformedToken);

        // ✅ THEN: Validation fails with invalid token error
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.InvalidToken);
        assertEquals("AUTH_001_INVALID_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Expired token validation
     */
    @Test
    void shouldFailValidationWithExpiredToken() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Expired JWT token
        String expiredToken = createExpiredTestToken("user-123", List.of("TRADER"));

        // ✅ WHEN: Validating expired token
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(expiredToken);

        // ✅ THEN: Validation fails with expired token error
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.ExpiredToken);
        assertEquals("AUTH_002_EXPIRED_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Token generation with custom claims
     */
    @Test
    void shouldGenerateTokenWithCustomClaims() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: User details and custom claims
        String userId = "user-789";
        List<String> roles = List.of("ADMIN", "TRADER");
        Map<String, Object> customClaims = Map.of(
            "department", "trading",
            "clearance", "L3"
        );

        // ✅ WHEN: Generating token
        CompletableFuture<Result<String, GatewayError>> future = 
            jwtService.generateJwtToken(userId, roles, customClaims);

        // ✅ THEN: Token generated successfully with custom claims
        Result<String, GatewayError> result = future.get();
        assertTrue(result.isSuccess());
        
        String generatedToken = result.getValue().get();
        assertNotNull(generatedToken);
        assertFalse(generatedToken.isBlank());
        
        // Validate the generated token contains custom claims
        CompletableFuture<Result<JwtValidationResult, GatewayError>> validationFuture = 
            jwtService.validateJwtToken(generatedToken);
        Result<JwtValidationResult, GatewayError> validationResult = validationFuture.get();
        
        assertTrue(validationResult.isSuccess());
        JwtValidationResult validation = validationResult.getValue().get();
        assertEquals(userId, validation.userId());
        assertEquals(roles, validation.roles());
        assertEquals("trading", validation.allClaims().get("department"));
        assertEquals("L3", validation.allClaims().get("clearance"));
    }

    /**
     * ✅ TEST: Token blacklisting functionality
     */
    @Test
    void shouldBlacklistTokenSuccessfully() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Valid JWT token
        String validToken = createValidTestToken("user-123", List.of("TRADER"));

        // ✅ WHEN: Blacklisting token
        CompletableFuture<Result<Void, GatewayError>> blacklistFuture = 
            jwtService.blacklistToken(validToken);

        // ✅ THEN: Token blacklisted successfully
        Result<Void, GatewayError> blacklistResult = blacklistFuture.get();
        assertTrue(blacklistResult.isSuccess());

        // ✅ AND THEN: Subsequent validation fails
        CompletableFuture<Result<JwtValidationResult, GatewayError>> validationFuture = 
            jwtService.validateJwtToken(validToken);
        Result<JwtValidationResult, GatewayError> validationResult = validationFuture.get();
        
        assertTrue(validationResult.isFailure());
        GatewayError error = validationResult.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.InvalidToken);
        assertEquals("AUTH_001_INVALID_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Token with wrong signature validation
     */
    @Test
    void shouldFailValidationWithWrongSignature() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Token signed with different key
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-jwt-signing".getBytes());
        String wrongSignedToken = Jwts.builder()
            .setSubject("user-123")
            .setIssuer(testIssuer)
            .setAudience("event-bus")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
            .claim("roles", List.of("TRADER"))
            .claim("token_id", "test-token-id")
            .signWith(wrongKey, SignatureAlgorithm.HS256)
            .compact();

        // ✅ WHEN: Validating token with wrong signature
        CompletableFuture<Result<JwtValidationResult, GatewayError>> future = 
            jwtService.validateJwtToken(wrongSignedToken);

        // ✅ THEN: Validation fails with invalid token error
        Result<JwtValidationResult, GatewayError> result = future.get();
        assertTrue(result.isFailure());
        
        GatewayError error = result.getError().get();
        assertTrue(error instanceof GatewayError.AuthenticationError.InvalidToken);
        assertEquals("AUTH_001_INVALID_TOKEN", error.getErrorCode());
    }

    /**
     * ✅ TEST: Concurrent token validation
     */
    @Test
    void shouldHandleConcurrentTokenValidation() throws ExecutionException, InterruptedException {
        // ✅ GIVEN: Multiple valid tokens
        int concurrentCount = 50;
        CompletableFuture<Result<JwtValidationResult, GatewayError>>[] futures = 
            new CompletableFuture[concurrentCount];

        // ✅ WHEN: Validating tokens concurrently
        for (int i = 0; i < concurrentCount; i++) {
            String token = createValidTestToken("user-" + i, List.of("TRADER"));
            futures[i] = jwtService.validateJwtToken(token);
        }

        // ✅ THEN: All validations complete successfully
        CompletableFuture.allOf(futures).get();
        
        for (CompletableFuture<Result<JwtValidationResult, GatewayError>> future : futures) {
            Result<JwtValidationResult, GatewayError> result = future.get();
            assertTrue(result.isSuccess());
        }
    }

    // ✅ HELPER METHODS: Test token builders

    private String createValidTestToken(String userId, List<String> roles) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuer(testIssuer)
            .setAudience("event-bus")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
            .claim("roles", roles)
            .claim("token_id", "test-token-" + System.nanoTime())
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    private String createExpiredTestToken(String userId, List<String> roles) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuer(testIssuer)
            .setAudience("event-bus")
            .setIssuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
            .setExpiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
            .claim("roles", roles)
            .claim("token_id", "expired-token-" + System.nanoTime())
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }
}
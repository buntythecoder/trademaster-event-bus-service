package com.trademaster.eventbus.functional;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ RESULT TYPE TESTS: Functional Railway Programming Testing
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #20: >80% unit test coverage with functional test builders
 * - Functional programming test patterns
 * - Railway programming validation
 * - Monadic composition testing
 * - Pattern matching in assertions
 * 
 * TEST COVERAGE:
 * - Success and failure path testing
 * - Monadic map and flatMap operations
 * - Error transformation and recovery
 * - Chain composition and short-circuiting
 * - Optional integration
 * - Result utilities and helpers
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
class ResultTest {
    
    /**
     * ✅ TEST: Success result creation and access
     */
    @Test
    void shouldCreateSuccessResult() {
        // ✅ GIVEN: Success value
        String successValue = "test-success";
        
        // ✅ WHEN: Creating success result
        Result<String, String> result = Result.success(successValue);
        
        // ✅ THEN: Result contains success value
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Optional.of(successValue), result.getValue());
        assertEquals(Optional.empty(), result.getError());
        assertEquals(successValue, result.getOrElse("default"));
    }
    
    /**
     * ✅ TEST: Failure result creation and access
     */
    @Test
    void shouldCreateFailureResult() {
        // ✅ GIVEN: Failure error
        String errorValue = "test-error";
        
        // ✅ WHEN: Creating failure result
        Result<String, String> result = Result.failure(errorValue);
        
        // ✅ THEN: Result contains error value
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(Optional.empty(), result.getValue());
        assertEquals(Optional.of(errorValue), result.getError());
        assertEquals("default", result.getOrElse("default"));
    }
    
    /**
     * ✅ TEST: Map operation on success result
     */
    @Test
    void shouldMapSuccessResult() {
        // ✅ GIVEN: Success result with integer value
        Result<Integer, String> result = Result.success(42);
        
        // ✅ WHEN: Mapping value to string
        Result<String, String> mappedResult = result.map(value -> "Value: " + value);
        
        // ✅ THEN: Mapping successful with transformed value
        assertTrue(mappedResult.isSuccess());
        assertEquals("Value: 42", mappedResult.getValue().get());
    }
    
    /**
     * ✅ TEST: Map operation on failure result
     */
    @Test
    void shouldMapFailureResult() {
        // ✅ GIVEN: Failure result with error
        Result<Integer, String> result = Result.failure("calculation-error");
        
        // ✅ WHEN: Mapping value to string
        Result<String, String> mappedResult = result.map(value -> "Value: " + value);
        
        // ✅ THEN: Mapping preserves error without transformation
        assertTrue(mappedResult.isFailure());
        assertEquals("calculation-error", mappedResult.getError().get());
    }
    
    /**
     * ✅ TEST: FlatMap operation chaining success
     */
    @Test
    void shouldChainFlatMapOperationsSuccessfully() {
        // ✅ GIVEN: Success result with integer value
        Result<Integer, String> result = Result.success(10);
        
        // ✅ WHEN: Chaining flatMap operations
        Result<String, String> chainedResult = result
            .flatMap(value -> Result.success(value * 2))
            .flatMap(value -> Result.success(value + 5))
            .map(value -> "Final: " + value);
        
        // ✅ THEN: Chain successful with final transformed value
        assertTrue(chainedResult.isSuccess());
        assertEquals("Final: 25", chainedResult.getValue().get());
    }
    
    /**
     * ✅ TEST: FlatMap operation short-circuits on failure
     */
    @Test
    void shouldShortCircuitOnFailureInChain() {
        // ✅ GIVEN: Success result with integer value
        Result<Integer, String> result = Result.success(10);
        
        // ✅ WHEN: Chaining with failure in middle
        Result<String, String> chainedResult = result
            .flatMap(value -> Result.success(value * 2))
            .flatMap(value -> Result.<Integer, String>failure("middle-error"))
            .map(value -> "Final: " + value);
        
        // ✅ THEN: Chain short-circuits preserving first error
        assertTrue(chainedResult.isFailure());
        assertEquals("middle-error", chainedResult.getError().get());
    }
    
    /**
     * ✅ TEST: Error transformation with mapError
     */
    @Test
    void shouldTransformErrorWithMapError() {
        // ✅ GIVEN: Failure result with simple error
        Result<String, String> result = Result.failure("simple-error");
        
        // ✅ WHEN: Transforming error to structured error
        Result<String, StructuredError> mappedResult = result.mapError(error -> 
            new StructuredError("VALIDATION_ERROR", error, "Input validation failed"));
        
        // ✅ THEN: Error transformed successfully
        assertTrue(mappedResult.isFailure());
        StructuredError structuredError = mappedResult.getError().get();
        assertEquals("VALIDATION_ERROR", structuredError.code());
        assertEquals("simple-error", structuredError.originalMessage());
        assertEquals("Input validation failed", structuredError.description());
    }
    
    /**
     * ✅ TEST: Fold operation for result processing
     */
    @Test
    void shouldFoldResultToCommonType() {
        // ✅ GIVEN: Success and failure results
        Result<Integer, String> successResult = Result.success(42);
        Result<Integer, String> failureResult = Result.failure("error-message");
        
        // ✅ WHEN: Folding both results to strings
        String successOutput = successResult.fold(
            value -> "Success: " + value,
            error -> "Error: " + error
        );
        
        String failureOutput = failureResult.fold(
            value -> "Success: " + value,
            error -> "Error: " + error
        );
        
        // ✅ THEN: Both results folded to appropriate strings
        assertEquals("Success: 42", successOutput);
        assertEquals("Error: error-message", failureOutput);
    }
    
    /**
     * ✅ TEST: Side effects with onSuccess and onFailure
     */
    @Test
    void shouldExecuteSideEffectsConditionally() {
        // ✅ GIVEN: Success and failure results with side effect trackers
        StringBuilder successSideEffect = new StringBuilder();
        StringBuilder failureSideEffect = new StringBuilder();
        
        Result<String, String> successResult = Result.success("test-value");
        Result<String, String> failureResult = Result.failure("test-error");
        
        // ✅ WHEN: Applying side effects
        successResult
            .onSuccess(value -> successSideEffect.append("Success: ").append(value))
            .onFailure(error -> failureSideEffect.append("Failure: ").append(error));
        
        failureResult
            .onSuccess(value -> successSideEffect.append("Success: ").append(value))
            .onFailure(error -> failureSideEffect.append("Failure: ").append(error));
        
        // ✅ THEN: Only appropriate side effects executed
        assertEquals("Success: test-value", successSideEffect.toString());
        assertEquals("Failure: test-error", failureSideEffect.toString());
    }
    
    /**
     * ✅ TEST: Recovery from failure
     */
    @Test
    void shouldRecoverFromFailure() {
        // ✅ GIVEN: Failure result
        Result<String, String> failureResult = Result.failure("network-error");
        
        // ✅ WHEN: Recovering with default value
        Result<String, String> recoveredResult = failureResult.recover(error -> 
            "recovered-from-" + error);
        
        // ✅ THEN: Recovery successful with default value
        assertTrue(recoveredResult.isSuccess());
        assertEquals("recovered-from-network-error", recoveredResult.getValue().get());
    }
    
    /**
     * ✅ TEST: Recovery with another Result
     */
    @Test
    void shouldRecoverWithAnotherResult() {
        // ✅ GIVEN: Failure result
        Result<String, String> failureResult = Result.failure("primary-failure");
        
        // ✅ WHEN: Recovering with another Result
        Result<String, String> recoveredResult = failureResult.recoverWith(error -> 
            Result.success("backup-value"));
        
        // ✅ THEN: Recovery successful with backup Result
        assertTrue(recoveredResult.isSuccess());
        assertEquals("backup-value", recoveredResult.getValue().get());
    }
    
    /**
     * ✅ TEST: Filter operation on success result
     */
    @Test
    void shouldFilterSuccessResult() {
        // ✅ GIVEN: Success result with integer value
        Result<Integer, String> result = Result.success(42);
        
        // ✅ WHEN: Filtering with passing predicate
        Result<Integer, String> passedFilter = result.filter(value -> value > 30, "too-small");
        
        // ✅ WHEN: Filtering with failing predicate
        Result<Integer, String> failedFilter = result.filter(value -> value > 50, "too-small");
        
        // ✅ THEN: Filter results appropriate
        assertTrue(passedFilter.isSuccess());
        assertEquals(42, passedFilter.getValue().get());
        
        assertTrue(failedFilter.isFailure());
        assertEquals("too-small", failedFilter.getError().get());
    }
    
    /**
     * ✅ TEST: Combine two success Results
     */
    @Test
    void shouldCombineTwoSuccessResults() {
        // ✅ GIVEN: Two success results
        Result<Integer, String> result1 = Result.success(10);
        Result<Integer, String> result2 = Result.success(20);
        
        // ✅ WHEN: Combining results
        Result<Integer, String> combinedResult = result1.combine(result2, Integer::sum);
        
        // ✅ THEN: Combination successful
        assertTrue(combinedResult.isSuccess());
        assertEquals(30, combinedResult.getValue().get());
    }
    
    /**
     * ✅ TEST: Combine success and failure Results
     */
    @Test
    void shouldFailCombiningWithFailureResult() {
        // ✅ GIVEN: Success and failure results
        Result<Integer, String> successResult = Result.success(10);
        Result<Integer, String> failureResult = Result.failure("second-error");
        
        // ✅ WHEN: Combining results
        Result<Integer, String> combinedResult = successResult.combine(failureResult, Integer::sum);
        
        // ✅ THEN: Combination fails with second error
        assertTrue(combinedResult.isFailure());
        assertEquals("second-error", combinedResult.getError().get());
    }
    
    /**
     * ✅ TEST: GetOrThrow operation
     */
    @Test
    void shouldGetOrThrowFromResult() {
        // ✅ GIVEN: Success and failure results
        Result<String, String> successResult = Result.success("success-value");
        Result<String, String> failureResult = Result.failure("error-value");
        
        // ✅ WHEN/THEN: GetOrThrow on success returns value
        assertEquals("success-value", 
            successResult.getOrThrow(error -> new RuntimeException(error)));
        
        // ✅ WHEN/THEN: GetOrThrow on failure throws exception
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            failureResult.getOrThrow(error -> new RuntimeException(error)));
        
        assertEquals("error-value", exception.getMessage());
    }
    
    // ✅ TEST DATA: Supporting classes for testing
    
    private record StructuredError(
        String code,
        String originalMessage,
        String description
    ) {}
}
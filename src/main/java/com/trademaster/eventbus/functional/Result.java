package com.trademaster.eventbus.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * ✅ FUNCTIONAL: Result Type for Railway Programming
 * 
 * MANDATORY COMPLIANCE:
 * - Functional programming with monadic composition
 * - No if-else statements in business logic
 * - Immutable sealed interface hierarchy
 * - Pattern matching for result processing
 * - Railway programming for error handling
 * 
 * FEATURES:
 * - Type-safe error handling without exceptions
 * - Chainable operations with flatMap/map
 * - Pattern matching support for switch expressions
 * - Optional integration for null safety
 * - Functional composition support
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per interface
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    // ✅ IMMUTABLE: Success case
    record Success<T, E>(T value) implements Result<T, E> {}
    
    // ✅ IMMUTABLE: Failure case  
    record Failure<T, E>(E error) implements Result<T, E> {}
    
    // ✅ FACTORY: Create success result
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    // ✅ FACTORY: Create failure result
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    // ✅ FUNCTIONAL: Transform success value
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> success -> Result.success(mapper.apply(success.value()));
            case Failure<T, E> failure -> Result.failure(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Chain operations (flatMap)
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> success -> mapper.apply(success.value());
            case Failure<T, E> failure -> Result.failure(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Transform error
    default <F> Result<T, F> mapError(Function<E, F> errorMapper) {
        return switch (this) {
            case Success<T, E> success -> Result.success(success.value());
            case Failure<T, E> failure -> Result.failure(errorMapper.apply(failure.error()));
        };
    }
    
    // ✅ FUNCTIONAL: Fold operation (catamorphism)
    default <U> U fold(Function<T, U> successMapper, Function<E, U> errorMapper) {
        return switch (this) {
            case Success<T, E> success -> successMapper.apply(success.value());
            case Failure<T, E> failure -> errorMapper.apply(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Side effects on success
    default Result<T, E> onSuccess(Consumer<T> consumer) {
        if (this instanceof Success<T, E> success) {
            consumer.accept(success.value());
        }
        return this;
    }
    
    // ✅ FUNCTIONAL: Side effects on failure
    default Result<T, E> onFailure(Consumer<E> consumer) {
        if (this instanceof Failure<T, E> failure) {
            consumer.accept(failure.error());
        }
        return this;
    }
    
    // ✅ FUNCTIONAL: Check if result is success
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    // ✅ FUNCTIONAL: Check if result is failure
    default boolean isFailure() {
        return this instanceof Failure;
    }
    
    // ✅ FUNCTIONAL: Get value as Optional
    default Optional<T> getValue() {
        return switch (this) {
            case Success<T, E> success -> Optional.of(success.value());
            case Failure<T, E> failure -> Optional.empty();
        };
    }
    
    // ✅ FUNCTIONAL: Get error as Optional
    default Optional<E> getError() {
        return switch (this) {
            case Success<T, E> success -> Optional.empty();
            case Failure<T, E> failure -> Optional.of(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Get value or default
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E> success -> success.value();
            case Failure<T, E> failure -> defaultValue;
        };
    }
    
    // ✅ FUNCTIONAL: Get value or throw
    default T getOrThrow(Function<E, RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success<T, E> success -> success.value();
            case Failure<T, E> failure -> throw exceptionMapper.apply(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Recover from failure
    default Result<T, E> recover(Function<E, T> recoveryFunction) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E> failure -> Result.success(recoveryFunction.apply(failure.error()));
        };
    }
    
    // ✅ FUNCTIONAL: Recover with another Result
    default Result<T, E> recoverWith(Function<E, Result<T, E>> recoveryFunction) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E> failure -> recoveryFunction.apply(failure.error());
        };
    }
    
    // ✅ FUNCTIONAL: Filter success values
    default Result<T, E> filter(java.util.function.Predicate<T> predicate, E errorOnFalse) {
        return switch (this) {
            case Success<T, E> success -> 
                predicate.test(success.value()) ? success : Result.failure(errorOnFalse);
            case Failure<T, E> failure -> failure;
        };
    }
    
    // ✅ FUNCTIONAL: Combine two Results
    default <U, V> Result<V, E> combine(Result<U, E> other, java.util.function.BiFunction<T, U, V> combiner) {
        return switch (this) {
            case Success<T, E> thisSuccess -> switch (other) {
                case Success<U, E> otherSuccess -> 
                    Result.success(combiner.apply(thisSuccess.value(), otherSuccess.value()));
                case Failure<U, E> otherFailure -> Result.failure(otherFailure.error());
            };
            case Failure<T, E> thisFailure -> Result.failure(thisFailure.error());
        };
    }
}

/**
 * ✅ UTILITY: Result utilities for common operations
 */
class ResultUtils {
    
    // ✅ FUNCTIONAL: Try operation with exception handling
    public static <T> Result<T, String> tryOperation(java.util.concurrent.Callable<T> operation) {
        try {
            return Result.success(operation.call());
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }
    
    // ✅ FUNCTIONAL: Sequence a list of Results (Fixed: Using proper stream folding)
    public static <T, E> Result<java.util.List<T>, E> sequence(java.util.List<Result<T, E>> results) {
        java.util.List<T> values = new java.util.ArrayList<>();
        
        return results.stream()
            .map(result -> result.fold(
                value -> { values.add(value); return Result.<Boolean, E>success(true); },
                error -> Result.<Boolean, E>failure(error)
            ))
            .filter(result -> result instanceof Result.Failure)
            .map(failureResult -> failureResult.fold(
                _ -> Result.<java.util.List<T>, E>success(values),
                error -> Result.<java.util.List<T>, E>failure(error)
            ))
            .findFirst()
            .orElse(Result.success(java.util.List.copyOf(values)));
    }
    
    // ✅ FUNCTIONAL: Traverse a list with a function returning Result
    public static <T, U, E> Result<java.util.List<U>, E> traverse(
            java.util.List<T> list, 
            Function<T, Result<U, E>> mapper) {
        
        return sequence(list.stream()
            .map(mapper)
            .collect(java.util.stream.Collectors.toList()));
    }
}
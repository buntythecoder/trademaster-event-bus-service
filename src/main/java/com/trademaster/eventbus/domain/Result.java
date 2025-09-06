package com.trademaster.eventbus.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Functional Result type for error handling without exceptions.
 * Supports railway-oriented programming patterns.
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Create a successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create a failed result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Check if result is successful
     */
    boolean isSuccess();
    
    /**
     * Check if result is failure
     */
    default boolean isFailure() {
        return !isSuccess();
    }
    
    /**
     * Get success value if present
     */
    Optional<T> getSuccess();
    
    /**
     * Get error value if present
     */
    Optional<E> getError();
    
    /**
     * Transform success value (flatMap for Result)
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    
    /**
     * Transform success value (map for Result)
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * Transform error value
     */
    <F> Result<T, F> mapError(Function<E, F> mapper);
    
    /**
     * Execute action if successful
     */
    Result<T, E> onSuccess(Consumer<T> action);
    
    /**
     * Execute action if failed
     */
    Result<T, E> onFailure(Consumer<E> action);
    
    /**
     * Get success value or throw exception
     */
    T orElseThrow(Function<E, RuntimeException> exceptionMapper);
    
    /**
     * Get success value or return default
     */
    T orElse(T defaultValue);
    
    /**
     * Fold result into single value (catamorphism)
     */
    <U> U fold(Function<E, U> errorMapper, Function<T, U> successMapper);
    
    /**
     * Success case
     */
    record Success<T, E>(T value) implements Result<T, E> {
        
        public Success {
            Objects.requireNonNull(value, "Success value cannot be null");
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public Optional<T> getSuccess() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<E> getError() {
            return Optional.empty();
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return success(value);
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            return this;
        }
        
        @Override
        public T orElseThrow(Function<E, RuntimeException> exceptionMapper) {
            return value;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return value;
        }
        
        @Override
        public <U> U fold(Function<E, U> errorMapper, Function<T, U> successMapper) {
            return successMapper.apply(value);
        }
    }
    
    /**
     * Failure case
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        
        public Failure {
            Objects.requireNonNull(error, "Error cannot be null");
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public Optional<T> getSuccess() {
            return Optional.empty();
        }
        
        @Override
        public Optional<E> getError() {
            return Optional.of(error);
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return failure(error);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return failure(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return failure(mapper.apply(error));
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            action.accept(error);
            return this;
        }
        
        @Override
        public T orElseThrow(Function<E, RuntimeException> exceptionMapper) {
            throw exceptionMapper.apply(error);
        }
        
        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public <U> U fold(Function<E, U> errorMapper, Function<T, U> successMapper) {
            return errorMapper.apply(error);
        }
    }
}
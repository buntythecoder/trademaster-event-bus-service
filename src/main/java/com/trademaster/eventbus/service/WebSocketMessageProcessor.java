package com.trademaster.eventbus.service;

import com.trademaster.eventbus.functional.Result;
import com.trademaster.eventbus.functional.GatewayError;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * ✅ SINGLE RESPONSIBILITY: WebSocket Message Processing
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility for message operations only
 * - Rule #3: Functional Programming - No if-else statements
 * - Rule #5: Cognitive Complexity ≤7 per method, ≤15 total per class
 * - Rule #12: Virtual Threads for all async operations
 * 
 * RESPONSIBILITIES:
 * - WebSocket message parsing and validation
 * - Message routing based on type
 * - Message format conversion
 * - Message delivery confirmation
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageProcessor {
    
    private final ObjectMapper objectMapper;
    private final EventCorrelationService correlationService;
    
    // ✅ VIRTUAL THREADS: Dedicated executor for message processing
    private final java.util.concurrent.Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * ✅ FUNCTIONAL: Process incoming WebSocket message
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Result<ProcessedMessage, GatewayError>> processIncomingMessage(
            String sessionId, 
            String rawMessage) {
        
        return CompletableFuture.supplyAsync(() -> 
            parseRawMessage(rawMessage)
                .flatMap(parsed -> validateMessageStructure(parsed, sessionId))
                .map(this::createProcessedMessage), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Send message to WebSocket session
     * Cognitive Complexity: 4
     */
    public CompletableFuture<Result<Void, GatewayError>> sendMessage(
            WebSocketSession session, 
            OutgoingMessage message) {
        
        return CompletableFuture.supplyAsync(() -> 
            serializeMessage(message)
                .flatMap(serialized -> deliverMessage(session, serialized))
                .map(this::logMessageDelivery), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Broadcast message to multiple sessions
     * Cognitive Complexity: 3
     */
    public CompletableFuture<Map<String, Result<Void, GatewayError>>> broadcastMessage(
            Map<String, WebSocketSession> sessions,
            OutgoingMessage message) {
        
        return CompletableFuture.supplyAsync(() -> 
            sessions.entrySet().stream()
                .collect(java.util.stream.Collectors.toConcurrentMap(
                    Map.Entry::getKey,
                    entry -> sendMessageSync(entry.getValue(), message)
                )), 
            virtualThreadExecutor);
    }
    
    /**
     * ✅ FUNCTIONAL: Create confirmation message
     * Cognitive Complexity: 2
     */
    public OutgoingMessage createConfirmationMessage(String sessionId, String userId) {
        return new OutgoingMessage(
            "connection_confirmed",
            Map.of(
                "session_id", sessionId,
                "user_id", userId,
                "timestamp", Instant.now().toString(),
                "status", "connected"
            ),
            MessagePriority.HIGH,
            Instant.now()
        );
    }
    
    // ✅ PRIVATE HELPERS: Single responsibility helper methods
    
    /**
     * ✅ FUNCTIONAL: Parse raw message from client
     * Cognitive Complexity: 3
     */
    private Result<ParsedMessage, GatewayError> parseRawMessage(String rawMessage) {
        return switch (validateMessageFormat(rawMessage)) {
            case VALID -> deserializeMessage(rawMessage);
            case EMPTY -> Result.failure(new GatewayError.MessageError.EmptyMessage(
                "WebSocket message is empty", "empty_context"));
            case INVALID_JSON -> Result.failure(new GatewayError.MessageError.InvalidFormat(
                "WebSocket message is not valid JSON", "message"));
            case TOO_LARGE -> Result.failure(new GatewayError.MessageError.MessageTooLarge(
                "WebSocket message exceeds size limit", 10241, 10240));
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Validate message format
     * Cognitive Complexity: 2
     */
    private MessageValidationStatus validateMessageFormat(String rawMessage) {
        return switch (rawMessage) {
            case null -> MessageValidationStatus.EMPTY;
            case String msg when msg.isBlank() -> MessageValidationStatus.EMPTY;
            case String msg when msg.length() > 10240 -> MessageValidationStatus.TOO_LARGE;
            case String msg when !msg.trim().startsWith("{") -> MessageValidationStatus.INVALID_JSON;
            default -> MessageValidationStatus.VALID;
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Deserialize JSON message
     * Cognitive Complexity: 2
     */
    private Result<ParsedMessage, GatewayError> deserializeMessage(String rawMessage) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(rawMessage, Map.class);
            
            ParsedMessage parsed = new ParsedMessage(
                (String) messageData.get("type"),
                (String) messageData.get("payload"),
                Instant.now()
            );
            
            return Result.success(parsed);
        } catch (Exception e) {
            log.error("Failed to parse WebSocket message: {}", e.getMessage());
            return Result.failure(new GatewayError.MessageError.InvalidFormat(
                "Message parsing failed: " + e.getMessage(), "message"));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Validate message structure
     * Cognitive Complexity: 2
     */
    private Result<ParsedMessage, GatewayError> validateMessageStructure(
            ParsedMessage parsed, 
            String sessionId) {
        
        return switch (parsed.type()) {
            case null -> Result.failure(new GatewayError.MessageError.MissingField(
                "Message type is required", "type"));
            case String type when type.isBlank() -> Result.failure(new GatewayError.MessageError.EmptyMessage(
                "Message type cannot be empty", "empty_type"));
            default -> Result.success(parsed);
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Create processed message
     * Cognitive Complexity: 1
     */
    private ProcessedMessage createProcessedMessage(ParsedMessage parsed) {
        return new ProcessedMessage(
            parsed.type(),
            parsed.payload(),
            parsed.receivedAt(),
            generateMessageId()
        );
    }
    
    /**
     * ✅ FUNCTIONAL: Serialize outgoing message
     * Cognitive Complexity: 2
     */
    private Result<String, GatewayError> serializeMessage(OutgoingMessage message) {
        try {
            String serialized = objectMapper.writeValueAsString(Map.of(
                "type", message.type(),
                "data", message.data(),
                "priority", message.priority().name(),
                "timestamp", message.timestamp().toString()
            ));
            return Result.success(serialized);
        } catch (Exception e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            return Result.failure(new GatewayError.SystemError.InternalServerError(
                "Message serialization failed: " + e.getMessage(), "MESSAGE_SERIALIZATION_ERROR"));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Deliver message to session
     * Cognitive Complexity: 3
     */
    private Result<Void, GatewayError> deliverMessage(WebSocketSession session, String serialized) {
        return session.isOpen() ?
            sendToSession(session, serialized) :
            Result.failure(new GatewayError.SystemError.WebSocketError(
                "WebSocket session is closed", session.getId()));
    }
    
    /**
     * ✅ FUNCTIONAL: Send to WebSocket session
     * Cognitive Complexity: 2
     */
    private Result<Void, GatewayError> sendToSession(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            return Result.failure(new GatewayError.SystemError.WebSocketError(
                "Failed to send WebSocket message: " + e.getMessage(), session.getId()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Synchronous message sending for broadcasting
     * Cognitive Complexity: 1
     */
    private Result<Void, GatewayError> sendMessageSync(WebSocketSession session, OutgoingMessage message) {
        return serializeMessage(message)
            .flatMap(serialized -> deliverMessage(session, serialized));
    }
    
    /**
     * ✅ FUNCTIONAL: Log message delivery
     * Cognitive Complexity: 1
     */
    private Void logMessageDelivery(Void result) {
        log.debug("WebSocket message delivered successfully");
        return null;
    }
    
    /**
     * ✅ FUNCTIONAL: Generate unique message ID
     * Cognitive Complexity: 1
     */
    private String generateMessageId() {
        return "msg_" + System.nanoTime() + "_" + Thread.currentThread().threadId();
    }
    
    // ✅ IMMUTABLE: Message records and enums
    
    public record ParsedMessage(
        String type,
        String payload,
        Instant receivedAt
    ) {}
    
    public record ProcessedMessage(
        String type,
        String payload,
        Instant receivedAt,
        String messageId
    ) {}
    
    public record OutgoingMessage(
        String type,
        Map<String, Object> data,
        MessagePriority priority,
        Instant timestamp
    ) {
        // Convenience methods for WebSocketEventRouter
        public String messageType() {
            return type;
        }
        
        public Map<String, Object> payload() {
            return data;
        }
    }
    
    public enum MessagePriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private enum MessageValidationStatus {
        VALID, EMPTY, INVALID_JSON, TOO_LARGE
    }
}
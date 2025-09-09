package com.trademaster.eventbus.common;

/**
 * Event Bus Exception
 * 
 * Custom exception for event bus service operations.
 * Provides structured error handling for event processing failures.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class EventBusException extends RuntimeException {
    
    private final String errorCode;
    private final String serviceId;
    private final String eventId;
    
    public EventBusException(String message) {
        super(message);
        this.errorCode = "EVENT_BUS_ERROR";
        this.serviceId = null;
        this.eventId = null;
    }
    
    public EventBusException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EVENT_BUS_ERROR";
        this.serviceId = null;
        this.eventId = null;
    }
    
    public EventBusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.serviceId = null;
        this.eventId = null;
    }
    
    public EventBusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.serviceId = null;
        this.eventId = null;
    }
    
    public EventBusException(String errorCode, String message, String serviceId, String eventId) {
        super(message);
        this.errorCode = errorCode;
        this.serviceId = serviceId;
        this.eventId = eventId;
    }
    
    public EventBusException(String errorCode, String message, String serviceId, String eventId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.serviceId = serviceId;
        this.eventId = eventId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EventBusException{");
        sb.append("errorCode='").append(errorCode).append('\'');
        if (serviceId != null) {
            sb.append(", serviceId='").append(serviceId).append('\'');
        }
        if (eventId != null) {
            sb.append(", eventId='").append(eventId).append('\'');
        }
        sb.append(", message='").append(getMessage()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
package com.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_events", indexes = {
    @Index(name = "idx_customer_events_customer_id", columnList = "customerId"),
    @Index(name = "idx_customer_events_timestamp", columnList = "timestamp"),
    @Index(name = "idx_customer_events_event_type", columnList = "eventType"),
    @Index(name = "idx_customer_events_channel", columnList = "channel")
})
public class CustomerEvent {

    public enum EventType {
        PAGE_VIEW, CLICK, PURCHASE, CART_ADD, CART_ABANDON, SEARCH, REVIEW, SIGNUP, LOGIN
    }

    public enum Channel {
        WEB, MOBILE, EMAIL, SMS, PUSH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(length = 128)
    private String sessionId;

    @Column(length = 64)
    private String productId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private Instant timestamp;

    private Instant processedAt;

    // Constructors
    public CustomerEvent() {}

    public CustomerEvent(String customerId, EventType eventType, Channel channel,
                         String sessionId, String productId, BigDecimal amount,
                         String metadata, Instant timestamp) {
        this.customerId = customerId;
        this.eventType = eventType;
        this.channel = channel;
        this.sessionId = sessionId;
        this.productId = productId;
        this.amount = amount;
        this.metadata = metadata;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

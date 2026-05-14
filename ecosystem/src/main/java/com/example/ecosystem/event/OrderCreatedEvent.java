package com.example.ecosystem.event;

public class OrderCreatedEvent {

    private final Long orderId;
    private final Long userId;
    private final Float totalAmount;

    public OrderCreatedEvent(Long orderId, Long userId, Float totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Float getTotalAmount() {
        return totalAmount;
    }
}
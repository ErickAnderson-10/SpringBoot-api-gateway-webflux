package com.reto.order_service.model;

public enum OrderStatus {
    PENDING, CONFIRMED, CANCELLED;

    public boolean canBeCancelled() {
        return this == PENDING || this == CONFIRMED;
    }
}
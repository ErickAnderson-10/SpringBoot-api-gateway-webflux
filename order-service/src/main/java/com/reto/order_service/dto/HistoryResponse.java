package com.reto.order_service.dto;

import com.reto.order_service.model.OrderStatus;
import com.reto.order_service.model.OrderStatusHistory;

import java.time.Instant;

public record HistoryResponse(OrderStatus fromStatus, OrderStatus toStatus, Instant changedAt) {

    public static HistoryResponse from(OrderStatusHistory h) {
        return new HistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedAt());
    }
}
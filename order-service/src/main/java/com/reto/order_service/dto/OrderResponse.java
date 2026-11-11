package com.reto.order_service.dto;

import com.reto.order_service.model.Order;
import com.reto.order_service.model.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String customerName,
        Long productId,
        Integer quantity,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerName(), o.getProductId(),
                o.getQuantity(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
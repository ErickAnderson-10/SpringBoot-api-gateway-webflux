package com.reto.inventory_service.dto;

public record AvailabilityResponse(
        Long productId,
        Integer requestedQuantity,
        Integer availableStock,
        boolean available) {
}

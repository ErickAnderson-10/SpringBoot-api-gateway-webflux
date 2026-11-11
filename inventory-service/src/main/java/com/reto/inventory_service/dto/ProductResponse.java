package com.reto.inventory_service.dto;

import com.reto.inventory_service.model.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, Integer stock, BigDecimal price) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getStock(), p.getPrice());
    }
}
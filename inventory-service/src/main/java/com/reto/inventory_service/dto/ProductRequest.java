package com.reto.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer stock,
        @NotNull @Positive BigDecimal price) {
}

package com.reto.order_service.controller;

import com.reto.order_service.dto.CreateOrderRequest;
import com.reto.order_service.dto.HistoryResponse;
import com.reto.order_service.dto.OrderResponse;
import com.reto.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un pedido",
            description = "Valida stock en Inventory. Si hay, queda CONFIRMED; si no, responde STOCK_INSUFFICIENT")
    public Mono<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Consultar un pedido por id")
    @GetMapping("/{id}")
    public Mono<OrderResponse> findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Consultar el historial de estados de un pedido")
    @GetMapping("/{id}/history")
    public Flux<HistoryResponse> history(@PathVariable Long id) {
        return service.findHistory(id);
    }

    @Operation(summary = "Cancelar un pedido", description = "Solo permitido desde PENDING o CONFIRMED")
    @PostMapping("/{id}/cancel")
    public Mono<OrderResponse> cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
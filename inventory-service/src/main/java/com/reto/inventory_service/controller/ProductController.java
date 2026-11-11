package com.reto.inventory_service.controller;

import com.reto.inventory_service.dto.AvailabilityResponse;
import com.reto.inventory_service.dto.ProductRequest;
import com.reto.inventory_service.dto.ProductResponse;
import com.reto.inventory_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Products")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @Operation(summary = "Registrar un producto",
            description = "Crea un producto con su nombre, stock inicial y precio")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Listar todos los productos")
    @GetMapping
    public Flux<ProductResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Consultar un producto por id",
            description = "Responde PRODUCT_NOT_FOUND (404) si no existe")
    @GetMapping("/{id}")
    public Mono<ProductResponse> findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Consultar disponibilidad de un producto",
            description = "Indica si hay stock suficiente para la cantidad pedida (parámetro quantity, por defecto 1)")
    @GetMapping("/{id}/availability")
    public Mono<AvailabilityResponse> availability(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int quantity) {
        return service.checkAvailability(id, quantity);
    }
}
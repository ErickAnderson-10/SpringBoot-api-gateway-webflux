package com.reto.inventory_service.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Producto no encontrado: " + id);
    }
}
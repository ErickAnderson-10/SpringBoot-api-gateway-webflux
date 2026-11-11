package com.reto.order_service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Pedido no encontrado: " + id);
    }
}
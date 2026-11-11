package com.reto.order_service.exception;

public class StockInsufficientException extends RuntimeException {
    public StockInsufficientException(Long productId) {
        super("Stock insuficiente para el producto " + productId);
    }
}
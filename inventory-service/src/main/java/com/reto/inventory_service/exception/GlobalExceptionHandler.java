package com.reto.inventory_service.exception;

import com.reto.inventory_service.config.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> productNotFound(ProductNotFoundException ex, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> validation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, exchange, null);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> badInput(ServerWebInputException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "El cuerpo o los parámetros de la petición no son válidos", exchange, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        String message = ex.getReason() != null ? ex.getReason() : "Solicitud no válida";
        return build(ex.getStatusCode(), "HTTP_" + ex.getStatusCode().value(), message, exchange, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, ServerWebExchange exchange) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error interno del servidor", exchange, ex);
    }

    private ResponseEntity<ApiError> build(HttpStatusCode status, String code, String message,
                                           ServerWebExchange exchange, Exception cause) {
        String traceId = exchange.getAttribute(TraceIdFilter.ATTR);
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, traceId);

        if (traceId != null) {
            MDC.put(TraceIdFilter.ATTR, traceId);
        }
        try {
            if (cause != null) {
                log.error("{} - {}", code, message, cause);
            } else {
                log.warn("{} - {}", code, message);
            }
        } finally {
            MDC.remove(TraceIdFilter.ATTR);
        }
        return ResponseEntity.status(status).body(body);
    }
}
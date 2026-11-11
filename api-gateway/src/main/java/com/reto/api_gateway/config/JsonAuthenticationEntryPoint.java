package com.reto.api_gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String traceId = exchange.getAttribute(TraceIdFilter.ATTR);

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"traceId\":\"%s\"}",
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "TOKEN_INVALID_OR_EXPIRED",
                "Token inválido, expirado o ausente",
                escape(traceId));

        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
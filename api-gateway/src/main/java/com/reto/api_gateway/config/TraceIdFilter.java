package com.reto.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class TraceIdFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Trace-Id";
    public static final String ATTR = "traceId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming;

        // El header se escribe en la petición que viaja hacia el microservicio
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, traceId)))
                .build();
        mutated.getAttributes().put(ATTR, traceId);
        mutated.getResponse().getHeaders().set(HEADER, traceId);

        String method = mutated.getRequest().getMethod().name();
        String path = mutated.getRequest().getURI().getPath();
        logWithTrace(traceId, "--> " + method + " " + path);

        return chain.filter(mutated)
                .contextWrite(ctx -> ctx.put(ATTR, traceId))
                .doFinally(signal -> logWithTrace(traceId,
                        "<-- " + method + " " + path + " status=" + mutated.getResponse().getStatusCode()));
    }

    private void logWithTrace(String traceId, String message) {
        MDC.put(ATTR, traceId);
        try {
            log.info(message);
        } finally {
            MDC.remove(ATTR);
        }
    }
}
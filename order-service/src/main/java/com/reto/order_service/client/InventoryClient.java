package com.reto.order_service.client;

import com.reto.order_service.config.TraceIdFilter;
import com.reto.order_service.dto.AvailabilityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class InventoryClient {

    private final WebClient webClient;

    public InventoryClient(@Value("${inventory.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .filter((request, next) -> Mono.deferContextual(ctx -> {
                    ClientRequest.Builder builder = ClientRequest.from(request);
                    if (ctx.hasKey(TraceIdFilter.ATTR)) {
                        String traceId = ctx.get(TraceIdFilter.ATTR);
                        builder.header(TraceIdFilter.HEADER, traceId);
                    }
                    return next.exchange(builder.build());
                }))
                .build();
    }

    public Mono<AvailabilityResponse> checkAvailability(Long productId, int quantity) {
        return webClient.get()
                .uri("/products/{id}/availability?quantity={q}", productId, quantity)
                .retrieve()
                .bodyToMono(AvailabilityResponse.class);
    }
}
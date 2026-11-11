package com.reto.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${services.order-url}") String orderUrl,
                               @Value("${services.inventory-url}") String inventoryUrl) {
        return builder.routes()
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .dedupeResponseHeader(TraceIdFilter.HEADER, "RETAIN_FIRST"))
                        .uri(orderUrl))
                .route("inventory-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .dedupeResponseHeader(TraceIdFilter.HEADER, "RETAIN_FIRST"))
                        .uri(inventoryUrl))
                .route("order-docs", r -> r
                        .path("/docs/orders")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(orderUrl))
                .route("inventory-docs", r -> r
                        .path("/docs/inventory")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(inventoryUrl))
                .build();
    }
}
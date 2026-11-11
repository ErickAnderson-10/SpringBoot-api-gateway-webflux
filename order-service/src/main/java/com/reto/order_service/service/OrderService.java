package com.reto.order_service.service;

import com.reto.order_service.client.InventoryClient;
import com.reto.order_service.dto.CreateOrderRequest;
import com.reto.order_service.dto.HistoryResponse;
import com.reto.order_service.dto.OrderResponse;
import com.reto.order_service.exception.InvalidTransitionException;
import com.reto.order_service.exception.OrderNotFoundException;
import com.reto.order_service.exception.StockInsufficientException;
import com.reto.order_service.mapper.OrderMapper;
import com.reto.order_service.model.Order;
import com.reto.order_service.model.OrderStatus;
import com.reto.order_service.model.OrderStatusHistory;
import com.reto.order_service.repository.OrderRepository;
import com.reto.order_service.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate tx;
    private final OrderMapper mapper;

    public Mono<OrderResponse> create(CreateOrderRequest req) {
        return inventoryClient.checkAvailability(req.productId(), req.quantity())
                .flatMap(availability -> {
                    if (!availability.available()) {
                        return Mono.error(new StockInsufficientException(req.productId()));
                    }
                    return Mono.fromCallable(() -> saveConfirmedOrder(req))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .map(mapper::toResponse);
    }

    public Mono<OrderResponse> findById(Long id) {
        return Mono.fromCallable(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new OrderNotFoundException(id)))
                .map(mapper::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<HistoryResponse> findHistory(Long id) {
        return Mono.fromCallable(() -> {
                    if (!orderRepository.existsById(id)) {
                        throw new OrderNotFoundException(id);
                    }
                    return historyRepository.findByOrderIdOrderByChangedAtAsc(id);
                })
                .flatMapMany(Flux::fromIterable)
                .map(mapper::toHistoryResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<OrderResponse> cancel(Long id) {
        return Mono.fromCallable(() -> cancelOrder(id))
                .map(mapper::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ---- métodos bloqueantes (se ejecutan en boundedElastic) ----

    private Order saveConfirmedOrder(CreateOrderRequest req) {
        return tx.execute(status -> {
            Instant now = Instant.now();
            Order order = orderRepository.save(Order.builder()
                    .customerName(req.customerName())
                    .productId(req.productId())
                    .quantity(req.quantity())
                    .status(OrderStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            recordHistory(order.getId(), null, OrderStatus.PENDING);

            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            recordHistory(order.getId(), OrderStatus.PENDING, OrderStatus.CONFIRMED);
            return order;
        });
    }

    private Order cancelOrder(Long id) {
        return tx.execute(status -> {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException(id));
            if (!order.getStatus().canBeCancelled()) {
                throw new InvalidTransitionException(
                        "No se puede cancelar un pedido en estado " + order.getStatus());
            }
            OrderStatus previous = order.getStatus();
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            recordHistory(order.getId(), previous, OrderStatus.CANCELLED);
            return order;
        });
    }

    private void recordHistory(Long orderId, OrderStatus from, OrderStatus to) {
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .changedAt(Instant.now())
                .build());
    }
}
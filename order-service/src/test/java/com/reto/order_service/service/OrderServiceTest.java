package com.reto.order_service.service;

import com.reto.order_service.client.InventoryClient;
import com.reto.order_service.dto.AvailabilityResponse;
import com.reto.order_service.dto.CreateOrderRequest;
import com.reto.order_service.exception.StockInsufficientException;
import com.reto.order_service.mapper.OrderMapper;
import com.reto.order_service.model.Order;
import com.reto.order_service.model.OrderStatus;
import com.reto.order_service.model.OrderStatusHistory;
import com.reto.order_service.repository.OrderRepository;
import com.reto.order_service.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.reto.order_service.exception.InvalidTransitionException;
import com.reto.order_service.exception.OrderNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock InventoryClient inventoryClient;
    @Mock TransactionTemplate tx;

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);
    private OrderService service;

    @BeforeEach
    void setUp() {
        lenient().when(tx.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));

        service = new OrderService(orderRepository, historyRepository,
                inventoryClient, tx, mapper);
    }

    // aquí van los tests
    @Test
    void create_withStock_savesOrderConfirmedAndRecordsHistory() {
        var req = new CreateOrderRequest("Erick", 1L, 2);
        when(inventoryClient.checkAvailability(1L, 2))
                .thenReturn(Mono.just(new AvailabilityResponse(1L, 2, 10, true)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(100L);
            return o;
        });

        StepVerifier.create(service.create(req))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(100L);
                    assertThat(r.status()).isEqualTo(OrderStatus.CONFIRMED);
                })
                .verifyComplete();

        ArgumentCaptor<OrderStatusHistory> captor =
                ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository, times(2)).save(captor.capture());
        List<OrderStatusHistory> h = captor.getAllValues();
        assertThat(h.get(0).getToStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(h.get(1).getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
    @Test
    void create_withoutStock_failsWithStockInsufficientAndSavesNothing() {
        var req = new CreateOrderRequest("Erick", 1L, 50);
        when(inventoryClient.checkAvailability(1L, 50))
                .thenReturn(Mono.just(new AvailabilityResponse(1L, 50, 10, false)));

        StepVerifier.create(service.create(req))
                .expectError(StockInsufficientException.class)
                .verify();

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    private Order order(Long id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .customerName("Erick")
                .productId(1L)
                .quantity(2)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private OrderStatusHistory history(Long orderId, OrderStatus from, OrderStatus to, Instant at) {
        return OrderStatusHistory.builder()
                .orderId(orderId).fromStatus(from).toStatus(to).changedAt(at).build();
    }
    @Test
    void findById_existing_returnsOrder() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order(1L, OrderStatus.CONFIRMED)));

        StepVerifier.create(service.findById(1L))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(1L);
                    assertThat(r.status()).isEqualTo(OrderStatus.CONFIRMED);
                })
                .verifyComplete();
    }

    @Test
    void findById_missing_failsWithOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(service.findById(99L))
                .expectError(OrderNotFoundException.class)
                .verify();
    }
    @Test
    void findHistory_existingOrder_returnsChangesInOrder() {
        Instant t = Instant.now();
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(historyRepository.findByOrderIdOrderByChangedAtAsc(1L)).thenReturn(List.of(
                history(1L, null, OrderStatus.PENDING, t),
                history(1L, OrderStatus.PENDING, OrderStatus.CONFIRMED, t.plusSeconds(1))));

        StepVerifier.create(service.findHistory(1L))
                .assertNext(h -> assertThat(h.toStatus()).isEqualTo(OrderStatus.PENDING))
                .assertNext(h -> {
                    assertThat(h.fromStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(h.toStatus()).isEqualTo(OrderStatus.CONFIRMED);
                })
                .verifyComplete();
    }

    @Test
    void findHistory_missingOrder_failsWithOrderNotFound() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        StepVerifier.create(service.findHistory(99L))
                .expectError(OrderNotFoundException.class)
                .verify();

        verifyNoInteractions(historyRepository);
    }
    @Test
    void cancel_confirmedOrder_becomesCancelledAndRecordsHistory() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order(1L, OrderStatus.CONFIRMED)));

        StepVerifier.create(service.cancel(1L))
                .assertNext(r -> assertThat(r.status()).isEqualTo(OrderStatus.CANCELLED))
                .verifyComplete();

        ArgumentCaptor<OrderStatusHistory> captor =
                ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(captor.getValue().getToStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_alreadyCancelled_failsWithInvalidTransition() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order(1L, OrderStatus.CANCELLED)));

        StepVerifier.create(service.cancel(1L))
                .expectError(InvalidTransitionException.class)
                .verify();

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void cancel_missingOrder_failsWithOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(service.cancel(99L))
                .expectError(OrderNotFoundException.class)
                .verify();
    }
    @Test
    void create_whenProductDoesNotExist_propagatesInventoryError() {
        var req = new CreateOrderRequest("Erick", 999L, 1);
        when(inventoryClient.checkAvailability(999L, 1))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        404, "Not Found", HttpHeaders.EMPTY, new byte[0], null)));

        StepVerifier.create(service.create(req))
                .expectError(WebClientResponseException.NotFound.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }
}
package com.reto.order_service.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING,   true",
            "CONFIRMED, true",
            "CANCELLED, false"
    })
    void canBeCancelled_dependsOnStatus(OrderStatus status, boolean expected) {
        assertThat(status.canBeCancelled()).isEqualTo(expected);
    }
}
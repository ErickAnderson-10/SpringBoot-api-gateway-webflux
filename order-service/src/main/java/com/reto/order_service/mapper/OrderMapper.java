package com.reto.order_service.mapper;

import com.reto.order_service.dto.HistoryResponse;
import com.reto.order_service.dto.OrderResponse;
import com.reto.order_service.model.Order;
import com.reto.order_service.model.OrderStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    HistoryResponse toHistoryResponse(OrderStatusHistory history);
}
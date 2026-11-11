package com.reto.inventory_service.mapper;

import com.reto.inventory_service.dto.ProductRequest;
import com.reto.inventory_service.dto.ProductResponse;
import com.reto.inventory_service.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequest request);
}
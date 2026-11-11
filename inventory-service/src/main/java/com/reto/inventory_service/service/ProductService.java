package com.reto.inventory_service.service;

import com.reto.inventory_service.dto.AvailabilityResponse;
import com.reto.inventory_service.dto.ProductRequest;
import com.reto.inventory_service.dto.ProductResponse;
import com.reto.inventory_service.exception.ProductNotFoundException;
import com.reto.inventory_service.mapper.ProductMapper;
import com.reto.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public Mono<ProductResponse> create(ProductRequest req) {
        return Mono.fromCallable(() -> repository.save(mapper.toEntity(req)))
                .map(mapper::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<ProductResponse> findAll() {
        return Mono.fromCallable(repository::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(mapper::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ProductResponse> findById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException(id)))
                .map(mapper::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AvailabilityResponse> checkAvailability(Long id, int quantity) {
        return Mono.fromCallable(() -> repository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException(id)))
                .map(p -> new AvailabilityResponse(
                        p.getId(), quantity, p.getStock(), p.getStock() >= quantity))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
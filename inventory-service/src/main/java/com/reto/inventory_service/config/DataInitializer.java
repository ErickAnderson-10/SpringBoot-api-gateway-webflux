package com.reto.inventory_service.config;

import com.reto.inventory_service.model.Product;
import com.reto.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                Product.builder().name("Laptop").stock(10).price(new BigDecimal("2500.00")).build(),
                Product.builder().name("Mouse").stock(50).price(new BigDecimal("45.90")).build(),
                Product.builder().name("Teclado").stock(30).price(new BigDecimal("120.00")).build()
        ));
        log.info("Productos de ejemplo creados");
    }
}
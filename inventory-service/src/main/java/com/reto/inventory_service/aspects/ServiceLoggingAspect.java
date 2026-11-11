package com.reto.inventory_service.aspect;

import com.reto.inventory_service.config.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Around("execution(public * com.reto.inventory_service.service..*(..))")
    @SuppressWarnings("unchecked")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();
        Object result = pjp.proceed();

        if (result instanceof Mono<?>) {
            Mono<Object> mono = (Mono<Object>) result;
            return Mono.deferContextual(ctx -> {
                long start = System.nanoTime();
                logWithTrace(ctx, method + " iniciado");
                return mono
                        .doOnSuccess(v -> logWithTrace(ctx, method + " completado en " + elapsed(start) + " ms"))
                        .doOnError(e -> logWithTrace(ctx, method + " falló en " + elapsed(start)
                                + " ms: " + e.getClass().getSimpleName()));
            });
        }

        if (result instanceof Flux<?>) {
            Flux<Object> flux = (Flux<Object>) result;
            return Flux.deferContextual(ctx -> {
                long start = System.nanoTime();
                logWithTrace(ctx, method + " iniciado");
                return flux
                        .doOnComplete(() -> logWithTrace(ctx, method + " completado en " + elapsed(start) + " ms"))
                        .doOnError(e -> logWithTrace(ctx, method + " falló en " + elapsed(start)
                                + " ms: " + e.getClass().getSimpleName()));
            });
        }

        return result;
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void logWithTrace(ContextView ctx, String message) {
        String traceId = ctx.hasKey(TraceIdFilter.ATTR) ? ctx.get(TraceIdFilter.ATTR) : null;
        if (traceId != null) {
            MDC.put(TraceIdFilter.ATTR, traceId);
        }
        try {
            log.info(message);
        } finally {
            MDC.remove(TraceIdFilter.ATTR);
        }
    }
}
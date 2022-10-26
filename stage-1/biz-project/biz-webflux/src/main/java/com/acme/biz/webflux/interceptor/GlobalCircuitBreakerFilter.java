package com.acme.biz.webflux.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author jintaoZou
 * @date 2022/10/24-8:53
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class GlobalCircuitBreakerFilter implements WebFilter, InitializingBean, DisposableBean {

    private final Logger logger = LoggerFactory.getLogger(GlobalCircuitBreakerFilter.class);

    CircuitBreakerConfig config;

    CircuitBreakerRegistry registry;

    CircuitBreaker circuitBreaker;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        long start = System.nanoTime();
        return Mono.fromCallable(() -> chain.filter(exchange))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .flatMap((Function<Mono<Void>, Mono<Void>>) voidMono -> voidMono)
                .doOnSuccess(v -> circuitBreaker.onSuccess(System.nanoTime() - start, circuitBreaker.getTimestampUnit()))
                .doOnError(throwable -> {
                    logger.error("circuit error catch :{}", throwable);
                    circuitBreaker.onError(System.nanoTime() - start, circuitBreaker.getTimestampUnit(), throwable);
                });
    }


    @Override
    public void afterPropertiesSet() {
        config = countBasedConfig(1000, 30f);

        registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker(GlobalCircuitBreakerFilter.class.getSimpleName(), config);
    }

    private CircuitBreakerConfig countBasedConfig(int windowSize, float failureRate) {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(windowSize)
                .failureRateThreshold(failureRate)
                .ignoreExceptions(TimeoutException.class)
                .writableStackTraceEnabled(false)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Override
    public void destroy() {
        registry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }


}

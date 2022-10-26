package com.acme.biz.webflux.interceptor.retry;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author jintaoZou
 * @date 2022/10/26-9:30
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class GlobalRetryFilter implements WebFilter, InitializingBean {


    Retry retry;
    RetryConfig config;
    RetryRegistry registry;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.fromCallable(() -> chain.filter(exchange))
                .transform(RetryOperator.of(retry))
                .flatMap(v -> v)
                .doOnSuccess(v -> log.info("request [{}] do a retry!",
                        exchange.getRequest().getPath().pathWithinApplication().value()))
                .doOnError(t -> log.error("retry error : {}", t));
    }


    @Override
    public void afterPropertiesSet() throws Exception {

        config = RetryConfig.custom().maxAttempts(0).build();
        registry = RetryRegistry.of(config);
        retry = registry.retry(GlobalRetryFilter.class.getSimpleName(), config);
    }
}

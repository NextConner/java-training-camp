package com.acme.biz.webflux.interceptor.bulkhead;

import com.acme.biz.webflux.interceptor.GenericWebFilter;
import com.acme.biz.webflux.interceptor.GlobalCircuitBreakerFilter;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * @author jintaoZou
 * @date 2022/10/26-8:26
 */

@Slf4j
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GlobalBulkheadFilter implements GenericWebFilter<Bulkhead> {


    BulkheadConfig bulkheadConfig;
    /**
     * 使用 maxConcurrentCalls 作为许可数的bulkhead
     */
    SemaphoreBulkhead bulkhead;

    @PostConstruct
    public void init() {

        bulkheadConfig = BulkheadConfig
                .custom()
                .maxConcurrentCalls(200)
                .writableStackTraceEnabled(false)
                .fairCallHandlingStrategyEnabled(true)
                .maxWaitDuration(Duration.ofSeconds(10))
                .build();
        bulkhead = new SemaphoreBulkhead(GlobalCircuitBreakerFilter.class.getSimpleName(), bulkheadConfig);
    }

    /**
     * 基于线程池的隔离
     * @return
     */
    private ThreadPoolBulkheadConfig threadPoolBulkheadConfig() {
        return ThreadPoolBulkheadConfig
                .custom()
                .coreThreadPoolSize(5)
                .maxThreadPoolSize(10)
                .keepAliveDuration(Duration.ofMillis(3000))
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .queueCapacity(500)
                .writableStackTraceEnabled(false)
                .build();
    }

    ThreadPoolBulkheadRegistry poolBulkheadRegistry(Supplier<ThreadPoolBulkheadConfig> configSupplier) {
        return ThreadPoolBulkheadRegistry.of(configSupplier.get());
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return Mono.fromCallable(() -> chain.filter(exchange))
                .transform(BulkheadOperator.of(bulkhead))
                .flatMap(v -> v)
                .doOnSuccess(v -> bulkhead.releasePermission())
                .doOnError(v -> {
                    log.error("bulkhead decorate filter error : {}", v);
                    bulkhead.releasePermission();
                });
    }


    @PreDestroy
    public void destroy() {
        bulkhead.releasePermission();
    }


}

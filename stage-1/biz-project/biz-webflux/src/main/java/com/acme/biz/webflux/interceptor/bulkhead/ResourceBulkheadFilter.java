package com.acme.biz.webflux.interceptor.bulkhead;

import com.acme.biz.webflux.interceptor.GenericWebFilter;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jintaoZou
 * @date 2022/10/26-8:26
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ResourceBulkheadFilter implements GenericWebFilter<Bulkhead>, ApplicationListener<ContextRefreshedEvent> {


    BulkheadConfig bulkheadConfig;

    BulkheadRegistry registry;

    Map<HandlerMethod, Bulkhead> bulkheadMap = new HashMap<>();


    @PostConstruct
    public void init() {

        bulkheadConfig = BulkheadConfig.custom().maxConcurrentCalls(200)
                .writableStackTraceEnabled(false)
                .fairCallHandlingStrategyEnabled(true)
                .maxWaitDuration(Duration.ofSeconds(10))
                .build();

        registry = BulkheadRegistry.of(bulkheadConfig);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        Bulkhead bulkhead = getCurrentOperator(exchange, chain);
        if (bulkhead != null) {
            return Mono.fromCallable(() -> chain.filter(exchange))
                    .transform(BulkheadOperator.of(bulkhead))
                    .flatMap(voidMono -> voidMono)
                    .doOnSuccess(v -> bulkhead.releasePermission())
                    .doOnError(v -> {
                        log.error("bulkhead decorate filter error : {}", v);
                        bulkhead.releasePermission();
                    });
        }

        return chain.filter(exchange);
    }

    @Override
    public Bulkhead getCurrentOperator(ServerWebExchange exchange, WebFilterChain chain) {
        return bulkheadMap.get(getHandleMethod(exchange, (DefaultWebFilterChain) chain));
    }

    @PreDestroy
    public void destroy() {
        bulkheadMap.values().forEach(Bulkhead::releasePermission);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        Map<String, RequestMappingHandlerMapping> mappingMap = event.getApplicationContext().getBeansOfType(RequestMappingHandlerMapping.class);

        mappingMap.values().forEach(mapping -> {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                RequestMappingInfo mappingInfo = entry.getKey();
                HandlerMethod handlerMethod = entry.getValue();
                bulkheadMap.put(handlerMethod, registry.bulkhead(mappingInfo.toString(), bulkheadConfig));
            }
        });

    }


}
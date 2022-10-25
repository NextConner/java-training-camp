package com.acme.biz.webflux.interceptor;

import com.acme.biz.webflux.consts.Consts;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.*;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author jintaoZou
 * @date 2022/10/24-8:53
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class MethodCircuitBreakerFilter implements WebFilter, InitializingBean, DisposableBean, ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory.getLogger(MethodCircuitBreakerFilter.class);

    CircuitBreakerConfig config;
    CircuitBreakerRegistry registry;
    Map<Method, CircuitBreaker> circuitBreakerMap = new HashMap<>();

    @Override
    public void destroy() throws Exception {
        circuitBreakerMap.values().forEach(CircuitBreaker::reset);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        config = CircuitBreakerConfig.custom()
                .failureRateThreshold(10f)
                .slowCallRateThreshold(10f)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(7)
                .recordException(e -> e instanceof Exception)
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();
        registry = CircuitBreakerRegistry.of(config);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        AtomicReference<Method> circuitMethod = getMethod(exchange, (DefaultWebFilterChain) chain);
        Method method = circuitMethod.get();

        if (circuitBreakerMap.containsKey(method)) {
            CircuitBreaker circuitBreaker = circuitBreakerMap.get(method);
            if (circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
                //断路器打开后，路由到fallback 方法
                exchange = rebuildExchange(exchange);
            }
            final long start = circuitBreaker.getCurrentTimestamp();
            ServerWebExchange finalExchange = exchange;
            return Mono.fromCallable(() -> chain.filter(finalExchange))
                    .transform(CircuitBreakerOperator.of(circuitBreaker))
                    .flatMap((Function<Mono<Void>, Mono<Void>>) voidMono -> voidMono)
                    .doOnSuccess(v -> {
                        circuitBreaker.releasePermission();
                        circuitBreaker.onSuccess(System.nanoTime() - start, circuitBreaker.getTimestampUnit());
                    })
                    .doOnError(throwable -> {
                        logger.error("circuit error catch :{}", throwable);
                        circuitBreaker.onError(System.nanoTime() - start, circuitBreaker.getTimestampUnit(), throwable);
                    });
        }
        return chain.filter(exchange);
    }

    private ServerWebExchange rebuildExchange(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest serverHttpRequest = new ServerHttpRequest() {
            @Override
            public String getId() {
                return request.getId() + "wrap";
            }

            @Override
            public RequestPath getPath() {
                return request.getPath();
            }

            @Override
            public MultiValueMap<String, String> getQueryParams() {
                return request.getQueryParams();
            }

            @Override
            public MultiValueMap<String, HttpCookie> getCookies() {
                return request.getCookies();
            }

            @Override
            public String getMethodValue() {
                return request.getMethodValue();
            }

            @Override
            public URI getURI() {
                return request.getURI();
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return request.getBody();
            }

            @Override
            public HttpHeaders getHeaders() {
                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                map.addAll(request.getHeaders());
                map.put(HttpHeaders.ACCEPT, Collections.singletonList(map.get(HttpHeaders.ACCEPT) + Consts.FALLBACK_HEADER_VERSION));
                return new HttpHeaders(map);
            }
        };
        exchange = exchange.mutate().request(serverHttpRequest).build();
        return exchange;
    }

    private AtomicReference<Method> getMethod(ServerWebExchange exchange, DefaultWebFilterChain chain) {
        DefaultWebFilterChain filterChain = chain;

        AtomicReference<Method> atomicReference = new AtomicReference<>();

        WebHandler webHandler = filterChain.getHandler();
        if (webHandler instanceof DispatcherHandler) {

            DispatcherHandler dispatcherHandler = (DispatcherHandler) webHandler;
            for (HandlerMapping handlerMapping : dispatcherHandler.getHandlerMappings()) {
                if (handlerMapping instanceof RequestMappingHandlerMapping) {
                    // 从 RequestMappingHandlerMapping 中取出 HandleMethod
                    RequestMappingHandlerMapping mapping = (RequestMappingHandlerMapping) handlerMapping;
                    Mono<HandlerMethod> handlerMethodMono = mapping.getHandlerInternal(exchange);
                    handlerMethodMono.subscribe(handlerMethod -> atomicReference.set(handlerMethod.getMethod()));
                    break;
                }
            }
        }
        return atomicReference;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        //获取 RequestMappingHandleMapping
        ApplicationContext context = event.getApplicationContext();
        Map<String, RequestMappingHandlerMapping> handlerMappingMap = context.getBeansOfType(RequestMappingHandlerMapping.class);
        for (RequestMappingHandlerMapping mapping : handlerMappingMap.values()) {

            // RequestMappingInfo 存储了mapping 的具体信息
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                RequestMappingInfo mappingInfo = entry.getKey();
                Method method = entry.getValue().getMethod();
                circuitBreakerMap.put(method, registry.circuitBreaker(mappingInfo.toString(), config));
            }

        }

    }


}

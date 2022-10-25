package com.acme.biz.webflux.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtil;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.AbstractHandlerMethodMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * @author jintaoZou
 * @date 2022/10/24-8:53
 */

@Order(Ordered.HIGHEST_PRECEDENCE +1)
@Component
public class WebFluxFilter implements WebFilter, InitializingBean, DisposableBean, ApplicationListener<ContextRefreshedEvent> {


    CircuitBreakerConfig config;

    CircuitBreakerRegistry registry;

    Map<Method, CircuitBreaker> circuitBreakerMap = new HashMap<>();


    @Override
    public void destroy() throws Exception {

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

        if (null != circuitMethod  ) {
            Method method = circuitMethod.get();

            if (circuitBreakerMap.containsKey(method)) {
                CircuitBreaker circuitBreaker = circuitBreakerMap.get(method);
                System.out.println(" 断路器 " + circuitBreaker);
                final long start = circuitBreaker.getCurrentTimestamp();
                try {
                    circuitBreaker.acquirePermission();
                    Mono<Void> call = circuitBreaker.decorateCallable(() -> chain.filter(exchange)).call();
                    final long duration = circuitBreaker.getCurrentTimestamp() - start;
                    circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                    return call;
                } catch (Exception e) {
                    long duration = circuitBreaker.getCurrentTimestamp() - start;
                    circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), e);
                }
            }
        }

        return chain.filter(exchange);

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

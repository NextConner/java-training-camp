package com.acme.biz.webflux.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * @author jintaoZou
 * @date 2022/10/24-8:53
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class WebFluxFilter implements WebFilter, InitializingBean, DisposableBean, ApplicationListener<ContextRefreshedEvent> {


    CircuitBreakerConfig config;

    CircuitBreakerRegistry registry;

    Map<Method, RequestMappingInfo> circuitBreakerMap = new HashMap<>();


    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
                .recordException(e -> e instanceof Exception)
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();
        registry = CircuitBreakerRegistry.of(config);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {


        WebHandler handler = ((DefaultWebFilterChain) chain).getHandler();
        if (handler instanceof DispatcherHandler) {

            DispatcherHandler dispatcherHandler = (DispatcherHandler) handler;
            Optional<HandlerMapping> handlerMapping = dispatcherHandler.getHandlerMappings()
                    .stream().filter(mapping -> mapping instanceof RequestMappingHandlerMapping)
                    .findFirst();

            if (handlerMapping.isPresent()) {
                RequestMappingHandlerMapping mapping = (RequestMappingHandlerMapping) handlerMapping.get();

                mapping
                        .getHandlerInternal(exchange)
                        .mapNotNull(handlerMethod -> handlerMethod.getMethod())
                        .subscribe(handlerMethod -> {
                            if (null != handlerMethod && circuitBreakerMap.containsKey(handlerMethod)) {
                                RequestMappingInfo mappingInfo = circuitBreakerMap.get(handlerMethod);
                                CircuitBreaker circuitBreaker = registry.find(mappingInfo.toString()).get();
                                circuitBreaker.acquirePermission();
                                final long start = circuitBreaker.getCurrentTimestamp();
                                try {
                                    circuitBreaker.decorateRunnable(() -> chain.filter(exchange)).run();
                                    final long duration = circuitBreaker.getCurrentTimestamp() - start;
                                    circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                                    return;
                                } catch (Exception e) {
                                    long duration = circuitBreaker.getCurrentTimestamp() - start;
                                    circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), e);
                                }
                            }
                        });
            /*    mapping.getHandlerInternal(exchange).subscribe(handlerMethod -> {
                    if (null != handlerMethod && circuitBreakerMap.containsKey(handlerMethod.getMethod())) {
                        RequestMappingInfo mappingInfo = circuitBreakerMap.get(handlerMethod.getMethod());
                        CircuitBreaker circuitBreaker = registry.find(mappingInfo.toString()).get();
                        circuitBreaker.acquirePermission();
                        final long start = circuitBreaker.getCurrentTimestamp();
                        try {
                            System.out.println(1/0);
                            circuitBreaker.decorateRunnable(() -> chain.filter(exchange)).run();
                            long duration = circuitBreaker.getCurrentTimestamp() - start;
                            circuitBreaker.onSuccess(duration, circuitBreaker.getTimestampUnit());
                            return;
                        } catch (Exception e) {
                            long duration = circuitBreaker.getCurrentTimestamp() - start;
                            circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(),e);
                            throw e;
                        }
                    }
                });
            */

            }
        }

        return chain.filter(exchange);

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
                Method handlerMethod = entry.getValue().getMethod();
                registry.circuitBreaker(mappingInfo.toString(), config);
                circuitBreakerMap.put(handlerMethod, mappingInfo);
            }

        }

    }


}

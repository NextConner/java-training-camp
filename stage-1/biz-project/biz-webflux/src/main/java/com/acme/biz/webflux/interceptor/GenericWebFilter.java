package com.acme.biz.webflux.interceptor;

import org.springframework.http.server.RequestPath;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.AbstractHandlerMethodMapping;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.DefaultWebFilterChain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jintaoZou
 * @date 2022/10/26-10:02
 */
public interface GenericWebFilter<T> extends WebFilter {

    /**
     * 通过 exchange 获取当前处理的 handleMethod
     *
     * @param exchange
     * @return
     */
    default HandlerMethod getHandleMethod(ServerWebExchange exchange, DefaultWebFilterChain chain) {

        AtomicReference<HandlerMethod> atomicReference = new AtomicReference<>();
        WebHandler webHandler = chain.getHandler();
        if (webHandler instanceof DispatcherHandler) {

            DispatcherHandler dispatcherHandler = (DispatcherHandler) webHandler;
            dispatcherHandler.getHandlerMappings()
                    .stream().filter(hap -> hap instanceof RequestMappingHandlerMapping)
                    .findFirst()
                    .map(ha -> ((RequestMappingHandlerMapping) ha).getHandlerInternal(exchange))
                    .get()
                    .subscribe(handlerMethod -> atomicReference.set(handlerMethod));
        }
        return atomicReference.get();
    }


    /**
     * 获取当前泛型实体
     *
     * @param exchange
     * @param chain
     * @return
     */
    default T getCurrentOperator(ServerWebExchange exchange, WebFilterChain chain){return null;}

//     default public HandlerMethod lookupHandlerMethod(ServerWebExchange exchange) throws Exception {
//         List<AbstractHandlerMethodMapping.Match> matches = new ArrayList<>();
//         List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(exchange);
//         if (directPathMatches != null) {
//             addMatchingMappings(directPathMatches, matches, exchange);
//         }
//         if (matches.isEmpty()) {
//             addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, exchange);
//         }
//         if (!matches.isEmpty()) {
//             Comparator<AbstractHandlerMethodMapping.Match> comparator = new AbstractHandlerMethodMapping.MatchComparator(getMappingComparator(exchange));
//             matches.sort(comparator);
//             AbstractHandlerMethodMapping.Match bestMatch = matches.get(0);
//             if (matches.size() > 1) {
//                 if (logger.isTraceEnabled()) {
//                     logger.trace(exchange.getLogPrefix() + matches.size() + " matching mappings: " + matches);
//                 }
//                 if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
//                     for (AbstractHandlerMethodMapping.Match match : matches) {
//                         if (match.hasCorsConfig()) {
//                             return PREFLIGHT_AMBIGUOUS_MATCH;
//                         }
//                     }
//                 }
//                 else {
//                     AbstractHandlerMethodMapping.Match secondBestMatch = matches.get(1);
//                     if (comparator.compare(bestMatch, secondBestMatch) == 0) {
//                         Method m1 = bestMatch.getHandlerMethod().getMethod();
//                         Method m2 = secondBestMatch.getHandlerMethod().getMethod();
//                         RequestPath path = exchange.getRequest().getPath();
//                         throw new IllegalStateException(
//                                 "Ambiguous handler methods mapped for '" + path + "': {" + m1 + ", " + m2 + "}");
//                     }
//                 }
//             }
////             handleMatch(bestMatch.mapping, bestMatch.getHandlerMethod(), exchange);
//             return bestMatch.getHandlerMethod();
//         }
//        
//     }

}

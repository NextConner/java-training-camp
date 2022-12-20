package com.acme.biz.api.micrometer.binder.redis;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import static com.acme.biz.api.micrometer.Micrometers.async;

public class RedisOpsSetCounterMetrics implements RequestInterceptor, MeterBinder {

    private static MeterRegistry meterRegistry;

    private static Counter redisOpsSetCounter;

    @Override
    public void apply(RequestTemplate template) {
        async(() -> {
            String redisMethod = template.methodMetadata().configKey();
        });
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        meterRegistry = registry;
        redisOpsSetCounter = Counter.builder("redis.ops.set").register(registry);
    }
}

package com.acme.biz.client.cloud.config;


import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.acme.biz.client.cloud.config.UserServiceRibbonClientConfiguration.applicationInfoManager;

public class RedisConnectionProxy implements MeterBinder {

    private static MeterRegistry registry;
    private static Counter redisCounter;
    private static Timer timer;
    private static AtomicInteger myGauge;
    private static Counter redisSetSuccessCounter;
    static final String BINDER_KEY = "redis.counter";


    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        this.registry = meterRegistry;
        this.redisCounter = Counter.builder(BINDER_KEY).register(registry);
        this.redisSetSuccessCounter = Counter.builder(BINDER_KEY + ".success").register(registry);
        this.timer = Timer.builder("redis.set.response.time").register(registry);
        this.myGauge = registry.gauge("redis.set.guage", new AtomicInteger(0));
    }


    public static byte[] get(@Argument(0) byte[] key, @SuperCall Callable<byte[]> callable) {
        try {
            byte[] call = callable.call();
            redisCounter.increment();

            Map<String, String> metadata = applicationInfoManager.getInfo().getMetadata();
            metadata.put("redis.count", String.valueOf(redisCounter.count()));
            metadata.put("redis.count.success", String.valueOf(redisSetSuccessCounter.count()));
            return call;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Boolean set(@Argument(0) byte[] key, @Argument(1) byte[] value,
                              @Origin Method method, @This Object obj, @SuperCall Callable<Boolean> callable) {
        try {
            Boolean call = timer.recordCallable(callable);
            redisCounter.increment();
            if (call) {
                redisSetSuccessCounter.increment();
                myGauge.set((int) ((redisSetSuccessCounter.count() / redisCounter.count()) * 100));
            }
            Map<String, String> metadata = applicationInfoManager.getInfo().getMetadata();
            metadata.put("redis.count", String.valueOf(redisCounter.count()));
            metadata.put("redis.count.success", String.valueOf(redisSetSuccessCounter.count()));
            return call;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

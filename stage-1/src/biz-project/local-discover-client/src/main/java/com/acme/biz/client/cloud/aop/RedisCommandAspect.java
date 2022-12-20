package com.acme.biz.client.cloud.aop;

import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class RedisCommandAspect implements MeterBinder {

    private MeterRegistry registry;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        this.registry = meterRegistry;
    }

    private Logger logger = LoggerFactory.getLogger(RedisCommandAspect.class);

    @Around("execution(* com.acme.biz.client.cloud.config.LettuceRedisConnectionWrapper.*(..))")
    public Object point(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        String command = args[0].toString();
        String name = command.trim().toUpperCase();
        CommandType commandType = CommandType.valueOf(name);
        if (commandType.equals(CommandType.SET)) {
            logger.info("SET COMMAND :{}", command);
        }

        Object redisResultObject = joinPoint.proceed(joinPoint.getArgs());
        //TODO 监听 RedisConnection 方法，实现监控 redis ops of set 的操作
        logger.info("redis result : {}", redisResultObject);
        return redisResultObject;
    }

}

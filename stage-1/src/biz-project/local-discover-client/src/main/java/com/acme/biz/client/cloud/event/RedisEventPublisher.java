package com.acme.biz.client.cloud.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Map;

public class RedisEventPublisher implements ApplicationListener<RedisOpsEvent> , ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onApplicationEvent(RedisOpsEvent event) {

        InvocationContext source = (InvocationContext) event.getSource();

        Map<String, Object> contextData = source.getContextData();
        Object[] parameters = source.getParameters();
        Object target = source.getTarget();
        Method method = source.getMethod();
        Object timer = source.getTimer();

    }
}

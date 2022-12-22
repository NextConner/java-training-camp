package com.acme.biz.client.cloud.event;

import org.springframework.context.ApplicationEvent;

public class RedisOpsEvent extends ApplicationEvent {

    private final ReflectiveMethodInvocationContext context;

    public RedisOpsEvent(ReflectiveMethodInvocationContext source) {
        super(source);
        this.context = source;
    }




}

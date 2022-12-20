package com.acme.biz.client.cloud.config;

import org.springframework.context.ApplicationEvent;

public class RedisOpsEvent extends ApplicationEvent {

    public RedisOpsEvent(Object source) {
        super(source);
    }
}

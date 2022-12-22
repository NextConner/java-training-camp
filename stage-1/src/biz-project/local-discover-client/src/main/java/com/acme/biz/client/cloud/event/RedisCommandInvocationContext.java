package com.acme.biz.client.cloud.event;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class RedisCommandInvocationContext {

    ReflectiveMethodInvocationContext invocationContext;

    RedisConnection redisConnection;

    RedisConnectionFactory connectionFactory;



}

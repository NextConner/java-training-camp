package com.acme.biz.client.cloud.config;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

public class LettuceConnectionFactoryWrapper extends LettuceConnectionFactory {

    private LettuceConnectionFactory factory;

    public LettuceConnectionFactoryWrapper(LettuceConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public RedisConnection getConnection() {
        RedisConnection connection = super.getConnection();
        return new LettuceRedisConnectionWrapper(connection);
    }

}

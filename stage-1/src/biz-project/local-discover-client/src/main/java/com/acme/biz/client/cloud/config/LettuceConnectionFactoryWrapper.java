package com.acme.biz.client.cloud.config;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class LettuceConnectionFactoryWrapper extends LettuceConnectionFactory {

    private final LettuceConnectionFactory factory;

    public LettuceConnectionFactoryWrapper(LettuceConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public RedisConnection getConnection() {

        RedisConnection connection = super.getConnection();
        LettuceRedisConnectionWrapper redisConnection = null;
        try {
            redisConnection = new ByteBuddy()
                    .subclass(LettuceRedisConnectionWrapper.class)
                    .method(named("get"))
                    .intercept(MethodDelegation.to(RedisConnectionProxy.class))
                    .method(named("set"))
                    .intercept(MethodDelegation.to(RedisConnectionProxy.class))
                    .make().load(getClass().getClassLoader()).getLoaded().newInstance();
            redisConnection.setDelegate(connection);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return redisConnection;
    }

}

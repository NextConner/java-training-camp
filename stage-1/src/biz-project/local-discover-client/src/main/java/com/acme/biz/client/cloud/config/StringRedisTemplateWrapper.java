package com.acme.biz.client.cloud.config;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.io.Closeable;
import java.util.List;

public class StringRedisTemplateWrapper extends RedisTemplate {

    private RedisTemplate redisTemplate;

    public StringRedisTemplateWrapper(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }


    @Override
    @Nullable
    public Object execute(RedisCallback action) {
        return redisTemplate.execute(action);
    }

    @Override
    @Nullable
    public Object execute(RedisCallback action, boolean exposeConnection) {
        return redisTemplate.execute(action, exposeConnection);
    }

    @Override
    @Nullable
    public Object execute(RedisCallback action, boolean exposeConnection, boolean pipeline) {
        return redisTemplate.execute(action, exposeConnection, pipeline);
    }

    @Override
    public Object execute(SessionCallback session) {
        return redisTemplate.execute(session);
    }

    @Override
    public List<Object> executePipelined(SessionCallback session) {
        return redisTemplate.executePipelined(session);
    }

    @Override
    public List<Object> executePipelined(SessionCallback session, RedisSerializer resultSerializer) {
        return redisTemplate.executePipelined(session, resultSerializer);
    }

    @Override
    public List<Object> executePipelined(RedisCallback action) {
        return redisTemplate.executePipelined(action);
    }

    @Override
    public List<Object> executePipelined(RedisCallback action, RedisSerializer resultSerializer) {
        return redisTemplate.executePipelined(action, resultSerializer);
    }

    @Override
    public Object execute(RedisScript script, List keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    @Override
    public Object execute(RedisScript script, RedisSerializer argsSerializer, RedisSerializer resultSerializer, List keys, Object... args) {
        return redisTemplate.execute(script, argsSerializer, resultSerializer, keys, args);
    }

    @Override
    public Closeable executeWithStickyConnection(RedisCallback callback) {
        return redisTemplate.executeWithStickyConnection(callback);
    }
}

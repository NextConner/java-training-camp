package com.acme.biz.client.cloud.config;


import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;

import java.util.List;

public class LettuceRedisConnectionWrapper extends AbstractRedisConnection{
    private RedisConnection delegate;

    public LettuceRedisConnectionWrapper(RedisConnection delegate) {
        this.delegate = delegate;
    }

    public LettuceRedisConnectionWrapper(){}

    public RedisConnection getDelegate() {
        return delegate;
    }

    public void setDelegate(RedisConnection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Boolean set(byte[] key, byte[] value) {
        return delegate.set(key, value);
    }

    @Override
    public byte[] get(byte[] key) {
        return delegate.get(key);
    }

    @Override
    public RedisGeoCommands geoCommands() {
        return delegate.geoCommands();
    }

    @Override
    public RedisHashCommands hashCommands() {
        return delegate.hashCommands();
    }

    @Override
    public RedisHyperLogLogCommands hyperLogLogCommands() {
        return delegate.hyperLogLogCommands();
    }

    @Override
    public RedisKeyCommands keyCommands() {
        return delegate.keyCommands();
    }

    @Override
    public RedisListCommands listCommands() {
        return delegate.listCommands();
    }

    @Override
    public RedisSetCommands setCommands() {
        return delegate.setCommands();
    }

    @Override
    public RedisScriptingCommands scriptingCommands() {
        return delegate.scriptingCommands();
    }

    @Override
    public RedisStringCommands stringCommands() {
        return delegate.stringCommands();
    }

    @Override
    public RedisServerCommands serverCommands() {
        return delegate.serverCommands();
    }

    @Override
    public RedisZSetCommands zSetCommands() {
        return delegate.zSetCommands();
    }

    @Override
    public Object execute(String command, byte[]... args) {
        return delegate.execute(command, args);
    }


    @Override
    public void close() throws DataAccessException {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public Object getNativeConnection() {
        return delegate.getNativeConnection();
    }


    @Override
    public boolean isQueueing() {
        return delegate.isQueueing();
    }

    @Override
    public boolean isPipelined() {
        return delegate.isPipelined();
    }

    @Override
    public void openPipeline() {
        delegate.openPipeline();
    }

    @Override
    public List<Object> closePipeline() {
        return delegate.closePipeline();
    }

    @Override
    public byte[] echo(byte[] message) {
        return delegate.echo(message);
    }

    @Override
    public String ping() {
        return delegate.ping();
    }

    @Override
    public void discard() {
        delegate.discard();
    }

    @Override
    public List<Object> exec() {
        return delegate.exec();
    }

    @Override
    public void multi() {
        delegate.multi();
    }

    @Override
    public void select(int dbIndex) {
        delegate.select(dbIndex);
    }

    @Override
    public void unwatch() {
        delegate.unwatch();
    }

    @Override
    public void watch(byte[]... keys) {
        delegate.watch(keys);
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        return delegate.publish(channel, message);
    }

    @Override
    public Subscription getSubscription() {
        return delegate.getSubscription();
    }

    @Override
    public boolean isSubscribed() {
        return delegate.isSubscribed();
    }

    @Override
    public void pSubscribe(MessageListener listener, byte[]... patterns) {
        delegate.pSubscribe(listener, patterns);
    }

    @Override
    public void subscribe(MessageListener listener, byte[]... channels) {
        delegate.subscribe(listener, channels);
    }


    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return delegate.getSentinelConnection();
    }


}

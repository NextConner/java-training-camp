package com.acme.biz.client.cloud.schedule;

import com.netflix.discovery.DiscoveryEvent;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.loadbalancer.ServerListUpdater;

import java.util.Date;

public class InstanceInfoUploader implements ServerListUpdater, EurekaEventListener {

    private final EurekaClient eurekaClient;

    private UpdateAction updateAction;
    private long timestamp;

    public InstanceInfoUploader(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
        // 注册当前对象作为 EurekaEventListener
        eurekaClient.registerEventListener(this);
    }

    @Override
    public void onEvent(EurekaEvent eurekaEvent) {

        if (eurekaEvent instanceof DiscoveryEvent) {
            DiscoveryEvent discoveryEvent = (DiscoveryEvent) eurekaEvent;
            this.timestamp = discoveryEvent.getTimestamp();
            updateAction.doUpdate();
        }

    }

    @Override
    public void start(UpdateAction updateAction) {
        this.updateAction = updateAction;
    }

    @Override
    public void stop() {

    }

    @Override
    public String getLastUpdate() {
        return new Date(this.timestamp).toString();
    }

    @Override
    public long getDurationSinceLastUpdateMs() {
        return 0;
    }

    @Override
    public int getNumberMissedCycles() {
        return 0;
    }

    @Override
    public int getCoreThreads() {
        return 0;
    }
}

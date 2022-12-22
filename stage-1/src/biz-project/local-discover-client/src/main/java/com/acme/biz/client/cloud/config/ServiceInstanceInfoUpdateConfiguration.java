/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.biz.client.cloud.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.util.Map;


@Configuration
public class ServiceInstanceInfoUpdateConfiguration {

    private Logger logger = LoggerFactory.getLogger(ServiceInstanceInfoUpdateConfiguration.class);


    private static final boolean HOTSPOT_JVM = ClassUtils.isPresent("com.sun.management.OperatingSystemMXBean", null);

    @Autowired
    private EurekaClient eurekaClient;

    @Autowired
    private StringRedisTemplateWrapper templateWrapper;

    public static ApplicationInfoManager applicationInfoManager;

    @PostConstruct
    public void init() {
        this.applicationInfoManager = eurekaClient.getApplicationInfoManager();
    }

    @Scheduled(fixedRate = 5000L, initialDelay = 10L)
    public void upload() {
        InstanceInfo instanceInfo = applicationInfoManager.getInfo();
        Map<String, String> metadata = instanceInfo.getMetadata();
        metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
        metadata.put("cpu-usage", String.valueOf(getCpuUsage()));

        instanceInfo.setIsDirty();
        logger.info("Upload Eureka InstanceInfo's metadata");
    }

    @Scheduled(fixedRate = 5000L, initialDelay = 10L)
    public void doCreateMeterInfo() {

        templateWrapper.opsForValue().set("name", "joker:" + System.nanoTime());
        logger.info("redis set name over!");
        String value = (String) templateWrapper.opsForValue().get("name");
        logger.info("setted value:{}", value);
    }

    /**
     * 基于默认的 MXBean 入口 {@link  ManagementFactory} 获取当前应用或系统的CPU使用率
     *
     * @return
     */
    private Integer getCpuUsage() {
        if (HOTSPOT_JVM) {
            OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            Double usage = operatingSystemMXBean.getProcessCpuLoad() * 100 * 100;
            return usage.intValue();
        } else {
            return 0;
        }
    }

}

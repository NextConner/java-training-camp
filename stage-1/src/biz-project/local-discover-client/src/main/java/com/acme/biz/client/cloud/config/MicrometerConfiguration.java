package com.acme.biz.client.cloud.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.cloud.client.serviceregistry.Registration;

import static java.util.Arrays.asList;

public class MicrometerConfiguration implements MeterRegistryCustomizer {


    @Value("${spring.application.name:default}")
    private String applicationName;

    @Autowired
    private Registration registration;

    @Override
    public void customize(MeterRegistry registry) {
        registry.config().commonTags(asList(
                Tag.of("application", applicationName), // 应用维度的 Tag
                Tag.of("host", registration.getHost())  // 应用 Host 的 Tag
        ));
    }

}

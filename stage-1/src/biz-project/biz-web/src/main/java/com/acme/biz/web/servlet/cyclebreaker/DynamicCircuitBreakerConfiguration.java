package com.acme.biz.web.servlet.cyclebreaker;

import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;

/**
 * 1.基于开放 circuit breaker endpoint 动态变更配置
 */
@Configuration
public class DynamicCircuitBreakerConfiguration {

    final static String CIRCUIT_BREAKER_PROPERTIES_PREFIX = "resilience4j.circuitbreaker.";

    @Autowired
    private ConfigurableEnvironment environment;

    private Binder binder;

    private CircuitBreakerProperties originalProperties;


    @PostConstruct
    public void init() {
        Iterable<ConfigurationPropertySource> configurationPropertySources = ConfigurationPropertySources.get(environment);
        binder = new Binder(configurationPropertySources);
        BindResult<CircuitBreakerProperties> result = binder.bind(CIRCUIT_BREAKER_PROPERTIES_PREFIX, CircuitBreakerProperties.class);
        buildOriginalCircuitBreakerConfig(result);
    }


    private void buildOriginalCircuitBreakerConfig(BindResult<CircuitBreakerProperties> result) {

        if (result.isBound()) {
            CircuitBreakerProperties circuitBreakerProperties = result.get();
            BeanUtils.copyProperties(circuitBreakerProperties,originalProperties);
//            Map<String, CircuitBreakerConfigurationProperties.InstanceProperties> configs = circuitBreakerProperties.getConfigs();
//            CircuitBreakerConfigurationProperties.InstanceProperties defaultConfig = configs.get("default");
//            System.out.println("default circuit config : " + defaultConfig);
        }
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChangeEvent(EnvironmentChangeEvent event) {

        // 需要排除非关注 keys
        // server.tomcat.*
        Set<String> keys = event.getKeys();
        // server.tomcat.threads.minSpare
        // server.tomcat.threads.max
        if (keys.contains(CIRCUIT_BREAKER_PROPERTIES_PREFIX + "failureRateThreshold")) {
            BindResult<CircuitBreakerProperties> result = binder.bind(CIRCUIT_BREAKER_PROPERTIES_PREFIX, CircuitBreakerProperties.class);
            buildOriginalCircuitBreakerConfig(result);
            CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = originalProperties.getConfigs().get("default");
            System.out.println("config  : " + instanceProperties.getFailureRateThreshold());
        }
    }



}

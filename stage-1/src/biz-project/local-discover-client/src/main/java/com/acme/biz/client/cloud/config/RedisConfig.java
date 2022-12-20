package com.acme.biz.client.cloud.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactoryWrapper(new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379)));
    }

    @Bean(name = "templateWrapper")
    public StringRedisTemplateWrapper stringRedisTemplate(RedisTemplate redisTemplate, LettuceConnectionFactory redisConnectionFactory) {
        StringRedisTemplateWrapper templateWrapper = new StringRedisTemplateWrapper(redisTemplate);
        templateWrapper.setConnectionFactory(redisConnectionFactory);
        return templateWrapper;
    }

}

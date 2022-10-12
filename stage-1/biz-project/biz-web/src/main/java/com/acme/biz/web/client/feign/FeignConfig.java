package com.acme.biz.web.client.feign;

import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jintaoZou
 * @date 2022/10/12-9:07
 */

@Configuration
public class FeignConfig {


    @Bean
    public Decoder decoder() {
        return new ApiResponseDecoder();
    }


}

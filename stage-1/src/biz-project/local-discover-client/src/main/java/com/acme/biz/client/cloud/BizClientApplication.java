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
package com.acme.biz.client.cloud;

import com.acme.biz.client.cloud.config.LettuceRedisConnectionWrapper;
import com.acme.biz.client.cloud.config.MicrometerConfiguration;
import com.acme.biz.client.cloud.config.UserServiceRibbonClientConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * biz-client 应用启动类
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@RestController
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Import(value = {MicrometerConfiguration.class})
@RibbonClient(name = "biz-client", configuration = UserServiceRibbonClientConfiguration.class)
public class BizClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizClientApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello , " + System.currentTimeMillis();
    }

}

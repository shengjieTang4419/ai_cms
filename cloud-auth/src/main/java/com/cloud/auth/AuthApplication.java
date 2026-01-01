package com.cloud.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/12/13 15:53
 */
@SpringBootApplication(scanBasePackages = {"com.cloud.common", "com.cloud.auth", "com.cloud.system.api"},
        exclude = DataSourceAutoConfiguration.class)
@EnableFeignClients(basePackages = "com.cloud.system.api.feign")
@EnableDiscoveryClient
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Auth模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}

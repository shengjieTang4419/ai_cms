package com.cloud.cloud.ai.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cloud.cloud.common", "com.cloud.cloud.ai"})
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
@EnableAsync
public class CloudAiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudAiChatApplication.class, args);
    }

}

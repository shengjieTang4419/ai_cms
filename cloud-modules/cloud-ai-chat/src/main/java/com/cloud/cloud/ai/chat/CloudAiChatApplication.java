package com.cloud.cloud.ai.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.cloud.cloud.common", "com.cloud.cloud.ai"})
@EnableAspectJAutoProxy(exposeProxy = true)
public class CloudAiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudAiChatApplication.class, args);
    }

}

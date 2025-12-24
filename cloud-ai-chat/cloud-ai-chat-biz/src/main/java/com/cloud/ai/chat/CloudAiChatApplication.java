package com.cloud.ai.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cloud.common", "com.cloud.ai"})
@EnableFeignClients(basePackages = {"com.cloud.*.api"})
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
@EnableAsync
public class CloudAiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudAiChatApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  AiChat模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }

}

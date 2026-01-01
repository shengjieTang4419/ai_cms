package com.cloud.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cloud.common", "com.cloud.membership"})
@EnableFeignClients(basePackages = {"com.cloud.*.api"})
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
@EnableAsync
public class CloudMembershipApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudMembershipApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  会员模块启动成功   ლ(´ڡ`ლ)ﾞ  ");

    }

}

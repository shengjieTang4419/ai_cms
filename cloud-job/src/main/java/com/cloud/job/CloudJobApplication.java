package com.cloud.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.cloud.common", "com.cloud.job"})
@EnableAspectJAutoProxy(exposeProxy = true)
public class CloudJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudJobApplication.class, args);
    }

}

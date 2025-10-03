package com.cloud.cloud.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 系统管理模块启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cloud.cloud.common", "com.cloud.cloud.system"})
@EnableAspectJAutoProxy(exposeProxy = true)
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}

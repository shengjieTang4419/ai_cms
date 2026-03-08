package com.cloud.ai.chat.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/5/20 11:09
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "chatTaskExecutor")
    public Executor defaultTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);          // 默认核心线程数
        executor.setMaxPoolSize(8);           // 默认最大线程数
        executor.setQueueCapacity(50);        // 队列容量
        executor.setThreadNamePrefix("async-chat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return TtlExecutors.getTtlExecutor(executor);
    }
}


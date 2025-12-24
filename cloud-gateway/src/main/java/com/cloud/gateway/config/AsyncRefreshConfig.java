package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步刷新线程池配置
 * 
 * @author shengjie.tang
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncRefreshConfig {

    /**
     * Token刷新专用线程池
     */
    @Bean("tokenRefreshExecutor")
    public Executor tokenRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数
        executor.setMaxPoolSize(5);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名前缀
        executor.setThreadNamePrefix("token-refresh-");
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Token刷新线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        
        return executor;
    }
}

package com.cloud.cloud.ai.chat.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 缓存配置 - 使用默认的内存缓存
 * @date 2025/10/24 00:00
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * ConcurrentMapCacheManager: Spring提供的基于ConcurrentHashMap的缓存实现
     * 
     * 特点：
     * 1. 简单、轻量级
     * 2. 无过期时间（永久缓存）
     * 3. 重启后数据丢失
     * 4. 适合数据量小、不常变化的场景
     */
    @Bean
    public CacheManager cacheManager() {
        // 创建缓存管理器，指定缓存名称
        // 这样就不会报 "Cannot find cache named 'xxx'" 错误了
        return new ConcurrentMapCacheManager(
            "occupations",  // 职业缓存
            "users",        // 用户缓存（预留）
            "sessions"      // 会话缓存（预留）
        );
    }
}


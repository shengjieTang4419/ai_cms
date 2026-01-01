package com.cloud.common.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 配置
 *
 * @author shengjie.tang
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();

        // 构建 Redis 地址
        String prefix = "redis://";
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String address = prefix + host + ":" + port;

        // 单机模式配置
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setPassword(StringUtils.hasText(redisProperties.getPassword()) ? redisProperties.getPassword() : null)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(3000)
                .setTimeout(3000);

        // 使用 Jackson 作为序列化器（性能好，可读性强）
        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }
}


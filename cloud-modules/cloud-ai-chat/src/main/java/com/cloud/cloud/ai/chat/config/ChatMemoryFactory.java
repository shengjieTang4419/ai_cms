package com.cloud.cloud.ai.chat.config;


import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/9/27 11:45
 */
@Configuration
@RequiredArgsConstructor
public class ChatMemoryFactory {

    private static final int MAX_MEMORY_MESSAGES = 20;
    private final RedisProperties redisProperties;

    @Bean
    public RedissonRedisChatMemoryRepository redisChatMemoryRepository() {
        return RedissonRedisChatMemoryRepository.builder()
                .host(redisProperties.getHost())
                .port(redisProperties.getPort())
                .password(redisProperties.getPassword())
                .build();
    }

    @Bean
    public MessageWindowChatMemory chatMemory(RedissonRedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(MAX_MEMORY_MESSAGES)
                .build();
    }

}

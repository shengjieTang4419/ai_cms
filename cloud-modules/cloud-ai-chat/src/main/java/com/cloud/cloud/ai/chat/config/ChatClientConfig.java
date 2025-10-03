package com.cloud.cloud.ai.chat.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: aiChat config
 * @date 2025/9/21 15:15
 */
@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {
    //默认聊天主体
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";
    private final ChatMemoryFactory chatMemoryFactory;
    private final RedissonRedisChatMemoryRepository redisChatMemoryRepository;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(redisChatMemoryRepository);

        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")
                        .withTopP(0.7)
                        .build())
                .build();
    }
}

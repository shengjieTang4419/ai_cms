package com.cloud.cloud.ai.chat.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import com.cloud.cloud.ai.chat.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    private final ChatMemoryFactory chatMemoryFactory;
    private final RedissonRedisChatMemoryRepository redisChatMemoryRepository;
    private final PromptLoader promptLoader;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(redisChatMemoryRepository);

        // 从资源文件加载系统提示词
        String systemPrompt = promptLoader.loadSystemPrompt();

        var chatClientBuilder = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")
                        .withTopP(0.7)
                        .build());

        //MCP注册
        if (tools != null && tools.getToolCallbacks().length > 0) {
            chatClientBuilder.defaultToolCallbacks(tools.getToolCallbacks());
        }

        return chatClientBuilder.build();
    }
}

package com.cloud.cloud.ai.chat.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
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

    //默认聊天主体
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个博学的智能聊天助手，能够帮助用户解答各种问题。

            你可以使用以下工具：
            - 天气查询工具：当用户询问天气相关信息时，你可以使用天气查询工具来获取指定城市的实时天气信息。

            请注意：
            1. 当用户询问天气时，主动使用天气查询工具获取最新信息
            2. 天气查询接受城市名称，无需城市编码，例如：上海、北京、浦东新区、上海市浦东新区等
            3. 系统会智能匹配城市名称到对应的城市编码
            4. 始终提供准确、实用的天气信息和建议

            请根据用户提问回答！
            """;
    private final ChatMemoryFactory chatMemoryFactory;
    private final RedissonRedisChatMemoryRepository redisChatMemoryRepository;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(redisChatMemoryRepository);

        var chatClientBuilder = builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
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

package com.cloud.ai.chat.helper;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.ai.chat.config.ChatMemoryFactory;
import com.cloud.ai.chat.util.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;

@Slf4j
public class ChatClientHelper {

    public static ChatClient buildChatClient(
            ChatClient.Builder chatClientBuilder,
            String modelName,
            ChatMemoryFactory chatMemoryFactory,
            PromptLoader promptLoader,
            ToolCallbackProvider toolCallbackProvider,
            String providerName) {

        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(
                chatMemoryFactory.redisChatMemoryRepository());
        String systemPrompt = promptLoader.loadSystemPrompt();

        var builder = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build());

        var optionsBuilder = DashScopeChatOptions.builder()
                .withModel(modelName)
                .withTopP(0.7);

        builder.defaultOptions(optionsBuilder.build());

        if (toolCallbackProvider != null && toolCallbackProvider.getToolCallbacks().length > 0) {
            builder.defaultToolCallbacks(toolCallbackProvider.getToolCallbacks());
            log.info("✅ 已注册 {} 个工具回调到{}模型", toolCallbackProvider.getToolCallbacks().length, providerName);
        } else {
            log.warn("⚠️ 未找到工具回调提供者或工具列表为空");
        }

        ChatClient chatClient = builder.build();
        log.info("✅ {}初始化完成，系统提示词长度: {} 字符", providerName, systemPrompt.length());

        return chatClient;
    }

    public static ChatClient buildSimpleChatClient(
            ChatClient.Builder chatClientBuilder,
            String modelName,
            ChatMemoryFactory chatMemoryFactory,
            PromptLoader promptLoader,
            String providerName) {

        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(
                chatMemoryFactory.redisChatMemoryRepository());
        String systemPrompt = promptLoader.loadSystemPrompt();

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(memory).build())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(modelName)
                        .withTopP(0.7)
                        .build())
                .build();

        log.info("✅ {}初始化完成", providerName);

        return chatClient;
    }
}

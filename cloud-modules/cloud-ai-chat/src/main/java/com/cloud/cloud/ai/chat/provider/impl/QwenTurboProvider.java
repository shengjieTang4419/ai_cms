package com.cloud.cloud.ai.chat.provider.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.cloud.ai.chat.config.ChatMemoryFactory;
import com.cloud.cloud.ai.chat.provider.ModelProvider;
import com.cloud.cloud.ai.chat.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 通义千问Turbo模型提供者
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 文本对话模型
 * @date 2025/10/14
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QwenTurboProvider implements ModelProvider {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemoryFactory chatMemoryFactory;
    private final PromptLoader promptLoader;
    private final ToolCallbackProvider toolCallbackProvider;

    private volatile ChatClient chatClientInstance;

    @Override
    public String getModelName() {
        return "qwen-turbo";
    }

    @Override
    public String getDisplayName() {
        return "通义千问Turbo";
    }

    @Override
    public boolean supportsVision() {
        return false;
    }

    @Override
    public boolean supportsStream() {
        return true;
    }

    @Override
    public ChatClient getChatClient() {
        if (chatClientInstance == null) {
            synchronized (this) {
                if (chatClientInstance == null) {
                    MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(
                            chatMemoryFactory.redisChatMemoryRepository());
                    String systemPrompt = promptLoader.loadSystemPrompt();

                    var builder = chatClientBuilder
                            .defaultSystem(systemPrompt)
                            .defaultAdvisors(
                                    new SimpleLoggerAdvisor(),
                                    MessageChatMemoryAdvisor.builder(memory).build())
                            .defaultOptions(DashScopeChatOptions.builder()
                                    .withModel(this.getModelName())
                                    .withTopP(0.7)
                                    .build());

                    // 注册MCP工具
                    if (toolCallbackProvider != null && toolCallbackProvider.getToolCallbacks().length > 0) {
                        builder.defaultToolCallbacks(toolCallbackProvider.getToolCallbacks());
                    }

                    chatClientInstance = builder.build();
                    log.info("✅ QwenTurboProvider初始化完成");
                }
            }
        }
        return chatClientInstance;
    }

    @Override
    public int getPriority() {
        return 10; // 默认优先级
    }
}


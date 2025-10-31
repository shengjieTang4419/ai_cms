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
import org.springframework.stereotype.Component;

/**
 * 通义千问VL-Plus模型提供者
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: Vision模型，支持图片输入
 * @date 2025/10/14
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QwenVisionProvider implements ModelProvider {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemoryFactory chatMemoryFactory;
    private final PromptLoader promptLoader;

    private volatile ChatClient chatClientInstance;

    @Override
    public String getModelName() {
        return "qwen-vl-plus";
    }

    @Override
    public String getDisplayName() {
        return "通义千问VL增强版";
    }

    @Override
    public boolean supportsVision() {
        return true;
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

                    chatClientInstance = chatClientBuilder
                            .defaultSystem(systemPrompt)
                            .defaultAdvisors(
                                    new SimpleLoggerAdvisor(),
                                    MessageChatMemoryAdvisor.builder(memory).build())
                            .defaultOptions(DashScopeChatOptions.builder()
                                    .withModel(this.getModelName())
                                    .withTopP(0.7)
                                    .build())
                            .build();

                    log.info("✅ QwenVisionProvider初始化完成");
                }
            }
        }
        return chatClientInstance;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}


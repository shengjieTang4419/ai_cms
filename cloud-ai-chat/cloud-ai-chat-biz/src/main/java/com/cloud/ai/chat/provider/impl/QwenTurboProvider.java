package com.cloud.ai.chat.provider.impl;

import com.cloud.ai.chat.config.ChatMemoryFactory;
import com.cloud.ai.chat.helper.ChatClientHelper;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.util.PromptLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClientHelper.buildChatClient(
                chatClientBuilder,
                getModelName(),
                chatMemoryFactory,
                promptLoader,
                toolCallbackProvider,
                QwenTurboProvider.class.getSimpleName()
        );
    }

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
        return chatClient;
    }

    @Override
    public int getPriority() {
        return 10;
    }
}


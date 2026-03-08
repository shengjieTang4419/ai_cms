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
 * 通义千问Thinking模型提供者
 * <p>
 * 支持思考推理过程输出的模型，优先级最高，默认使用
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: Thinking模型，支持推理过程输出
 * @date 2025/01/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QwenThinkingProvider implements ModelProvider {

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
                QwenThinkingProvider.class.getSimpleName()
        );
    }

    @Override
    public String getModelName() {
        return "qwen3-next-80b-a3b-thinking";
    }

    @Override
    public String getDisplayName() {
        return "通义千问Thinking（推理版）";
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
    public boolean supportsThinking() {
        return true;
    }

    @Override
    public boolean isMultiModel() {
        return true;
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }

    @Override
    public int getPriority() {
        return 3;
    }
}


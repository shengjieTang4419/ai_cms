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

    private volatile ChatClient chatClientInstance;

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
    public ChatClient getChatClient() {
        if (chatClientInstance == null) {
            synchronized (this) {
                if (chatClientInstance == null) {
                    MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(
                            chatMemoryFactory.redisChatMemoryRepository());
                    String systemPrompt = promptLoader.loadSystemPrompt();

                    var builder = chatClientBuilder
                            .defaultSystem(systemPrompt)
                            .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build());

                    // 构建DashScope选项，尝试设置enable_thinking参数
                    var optionsBuilder = DashScopeChatOptions.builder()
                            .withModel(this.getModelName())
                            .withTopP(0.7);

                    builder.defaultOptions(optionsBuilder.build());

                    // 注册MCP工具
                    if (toolCallbackProvider != null && toolCallbackProvider.getToolCallbacks().length > 0) {
                        builder.defaultToolCallbacks(toolCallbackProvider.getToolCallbacks());
                        log.info("✅ 已注册 {} 个工具回调到Thinking模型", toolCallbackProvider.getToolCallbacks().length);
                    } else {
                        log.warn("⚠️ 未找到工具回调提供者或工具列表为空");
                    }

                    chatClientInstance = builder.build();
                    log.info("✅ QwenThinkingProvider初始化完成，系统提示词长度: {} 字符", systemPrompt.length());
                }
            }
        }
        return chatClientInstance;
    }

    @Override
    public int getPriority() {
        return 1; // 最高优先级，确保默认使用
    }
}


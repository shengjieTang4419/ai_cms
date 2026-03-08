package com.cloud.ai.chat.provider.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.cloud.ai.chat.config.ChatMemoryFactory;
import com.cloud.ai.chat.helper.ChatClientHelper;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.util.PromptLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 通义千问Plus模型提供者
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: Plus模型
 * @date 2026/2/17 17:01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Qwen3_5PlusProvider implements ModelProvider {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemoryFactory chatMemoryFactory;
    private final PromptLoader promptLoader;
    private final ToolCallbackProvider toolCallbackProvider;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClientHelper.buildChatClient(
                chatClientBuilder,
                getModelName(),
                chatMemoryFactory,
                promptLoader,
                toolCallbackProvider,
                Qwen3_5PlusProvider.class.getSimpleName()
        );
    }

    @Override
    public String getModelName() {
        return "qwen3.5-plus";
    }

    @Override
    public String getDisplayName() {
        return "通义千问Plus";
    }

    @Override
    public boolean supportsThinking() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public boolean isMultiModel() {
        return true;
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
    public String simpleCall(String query) {
        try {
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(List.of(Collections.singletonMap("text", query)))
                    .build();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(getModelName())
                    .messages(Collections.singletonList(userMessage))
                    .build();
            MultiModalConversationResult result = conv.call(param);
            return String.valueOf(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
        } catch (Exception e) {
            log.error("多模态模型调用失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public int getPriority() {
        //我喜欢他的免费额度
        return 1;
    }
}

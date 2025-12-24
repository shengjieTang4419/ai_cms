package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.ai.chat.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 位置相关AI服务 - 智能判断是否需要位置信息
 * @date 2025/01/17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationAiService {

    private final ModelProviderManager providerManager;
    private final PromptLoader promptLoader;

    /**
     * 智能判断用户消息是否需要位置信息
     * 使用 AI 语义分析，比关键词匹配更准确
     *
     * @param userMessage 用户消息
     * @return true 需要位置信息，false 不需要位置信息
     */
    public boolean checkIfNeedLocation(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            log.debug("用户消息为空，不需要位置");
            return false;
        }

        try {
            // 加载位置判断提示词模板
            String promptTemplate = promptLoader.loadPrompt("prompts/location-check-prompt.txt");

            if (promptTemplate.isEmpty()) {
                log.warn("位置判断提示词加载失败，使用降级策略");
                return fallbackLocationCheck(userMessage);
            }

            // 构建完整 Prompt
            String fullPrompt = String.format("%s\n\n用户消息：%s", promptTemplate, userMessage);

            // 获取 ChatClient 调用 AI 判断
            ChatClient chatClient = providerManager.getDefaultProvider().getChatClient();
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();

            log.debug("AI 判断位置需求 - 用户消息: {}, AI 响应: {}", userMessage, response);

            // 判断响应中是否包含 true
            boolean needLocation = response.contains("true");
            log.info("位置需求判断: {} -> {}", userMessage, needLocation ? "需要" : "不需要");

            return needLocation;

        } catch (Exception e) {
            log.error("AI 判断位置需求失败，使用降级策略", e);
            return fallbackLocationCheck(userMessage);
        }
    }

    /**
     * 降级策略：使用简单关键词匹配
     * 当 AI 调用失败时使用
     *
     * @param message 用户消息
     * @return 是否需要位置
     */
    private boolean fallbackLocationCheck(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // 简单关键词匹配作为降级方案
        String[] needLocationKeywords = {
                "附近", "周边", "当前位置", "我在哪", "这附近",
                "怎么去", "怎么走", "路线", "导航"
        };

        String lowerMessage = message.toLowerCase();
        for (String keyword : needLocationKeywords) {
            if (lowerMessage.contains(keyword)) {
                // 排除明确指定起点终点的情况
                if (lowerMessage.contains("从") && (lowerMessage.contains("到") || lowerMessage.contains("去"))) {
                    log.debug("降级判断: 明确了起点终点，不需要位置");
                    return false;
                }
                log.debug("降级判断: 命中关键词 '{}', 需要位置", keyword);
                return true;
            }
        }

        log.debug("降级判断: 未命中任何关键词，不需要位置");
        return false;
    }
}

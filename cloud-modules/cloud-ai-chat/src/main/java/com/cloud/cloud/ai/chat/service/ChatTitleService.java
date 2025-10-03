package com.cloud.cloud.ai.chat.service;


import com.cloud.cloud.ai.chat.domain.ChatSession;
import com.cloud.cloud.ai.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/9/24 21:51
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTitleService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatClient chatClient;

    /**
     * 创建新会话并生成初始标题
     */
    @Async
    public void createSessionWithTitle(Long userId, String sessionId, String firstQuery) {
        try {

            // 1. 先创建会话记录，使用第一条问题作为临时标题
            ChatSession session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setFirstQuery(firstQuery);
            // 使用简单规则生成初始标题
            session.setTitle(generateSimpleTitle(firstQuery));
            chatSessionRepository.save(session);
            log.info("创建新会话: {}", sessionId);
            // 2. 异步生成更精准的智能标题
            generateIntelligentTitleAsync(sessionId, firstQuery);
        } catch (Exception e) {
            log.error("创建会话失败: {}", sessionId, e);
        }
    }

    /**
     * 智能生成标题（使用AI）
     */
    @Async
    public void generateIntelligentTitleAsync(String sessionId, String firstQuery) {
        try {
            String prompt = String.format(
                    "请为以下用户问题生成一个简短、有吸引力的对话标题（不超过15个字），要求：\n" +
                            "1. 简洁明了，能概括对话主题\n" +
                            "2. 不要包含'关于'、'讨论'等词\n" +
                            "3. 直接返回标题，不要额外解释\n\n" +
                            "用户问题：%s", firstQuery);

            String intelligentTitle = Objects.requireNonNull(chatClient.prompt(prompt)
                            .call()
                            .content())
                    .trim();

            // 更新会话标题
            Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                session.setTitle(intelligentTitle);
                chatSessionRepository.save(session);
                log.info("更新会话标题: {} -> {}", sessionId, intelligentTitle);
            }
        } catch (Exception e) {
            log.error("生成智能标题失败: {}", sessionId, e);
        }
    }

    /**
     * 基于规则生成简单标题（备用方案）
     */
    private String generateSimpleTitle(String firstQuery) {
        if (firstQuery.length() <= 15) {
            return firstQuery;
        }
        // 简单的标题生成规则
        String[] keyPhrases = {"介绍", "讲解", "如何", "什么是", "为什么", "怎样"};
        for (String phrase : keyPhrases) {
            if (firstQuery.contains(phrase)) {
                int start = firstQuery.indexOf(phrase);
                int end = Math.min(start + 12, firstQuery.length());
                return firstQuery.substring(start, end) + "...";
            }
        }

        // 默认截取前12个字符
        return firstQuery.substring(0, 12) + "...";
    }
}

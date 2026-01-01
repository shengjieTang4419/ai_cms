package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.domain.ChatMessage;
import com.cloud.ai.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 聊天消息服务
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 聊天消息业务逻辑
 * @date 2025/9/29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Value("${ai.guide:true}")
    private boolean aiGuide;

    /**
     * 保存用户消息
     */
    public void saveUserMessage(String sessionId, Long userId, String content) {
        saveUserMessage(sessionId, userId, content, null);
    }

    /**
     * 保存用户消息（支持图片URL列表）
     * 注意：OCR结果不存储在这里，只存储图片URL
     * OCR结果只在发送消息时用于LLM输入，不持久化
     */
    public void saveUserMessage(String sessionId, Long userId, String content, List<String> imageUrls) {
        saveUserMessage(sessionId, null, userId, content, imageUrls);
    }

    /**
     * 保存用户消息（支持dialogueId和图片URL列表）
     */
    public void saveUserMessage(String sessionId, String dialogueId, Long userId, String content, List<String> imageUrls) {
        try {
            ChatMessage message = new ChatMessage(sessionId, dialogueId, userId, ChatMessage.MessageType.USER, content, imageUrls);
            chatMessageRepository.save(message);
            log.debug("已保存用户消息到MongoDB: sessionId={}, dialogueId={}, imageUrls={}", sessionId, dialogueId, imageUrls != null ? imageUrls.size() : 0);
        } catch (Exception e) {
            log.error("保存用户消息到MongoDB失败: sessionId={}, dialogueId={}", sessionId, dialogueId, e);
        }
    }

    /**
     * 保存推荐消息
     * 只有在aiGuide开启且推荐列表不为空时才保存
     */
    public void saveRecommendationMessage(String sessionId, String dialogueId, Long userId, List<String> recommendations) {
        // 快速失败：功能未开启或推荐为空
        if (!aiGuide || recommendations == null || recommendations.isEmpty()) {
            log.debug("跳过保存推荐消息: aiGuide={}, recommendations={}", aiGuide, recommendations != null ? recommendations.size() : 0);
            return;
        }

        try {
            JSONArray recommendationsJson = new JSONArray(recommendations);
            ChatMessage message = new ChatMessage(sessionId, dialogueId, userId, ChatMessage.MessageType.RECOMMENDATIONS, recommendationsJson.toString());
            chatMessageRepository.save(message);
            log.debug("已保存推荐消息到MongoDB: sessionId={}, dialogueId={}, count={}", sessionId, dialogueId, recommendations.size());
        } catch (Exception e) {
            log.error("保存推荐消息到MongoDB失败: sessionId={}, dialogueId={}", sessionId, dialogueId, e);
        }
    }

    /**
     * 获取推荐消息
     * 从MongoDB中查询指定对话的推荐列表
     *
     * @param sessionId  会话ID
     * @param dialogueId 对话ID
     * @param userId     用户ID
     * @return 推荐列表，如果不存在则返回空列表
     */
    public List<String> getRecommendationMessage(String sessionId, String dialogueId, Long userId) {
        // 快速失败：功能未开启
        if (!aiGuide) {
            return new ArrayList<>();
        }

        try {
            List<ChatMessage> chatMessages = chatMessageRepository.findBySessionIdAndDialogueIdAndUserId(sessionId, dialogueId, userId);

            // 查找RECOMMENDATIONS类型的消息
            Optional<ChatMessage> recommendationMessage = chatMessages.stream()
                    .filter(msg -> ChatMessage.MessageType.RECOMMENDATIONS.equals(msg.getMessageType()))
                    .findFirst();

            if (recommendationMessage.isPresent()) {
                String content = recommendationMessage.get().getContent();
                JSONArray jsonArray = new JSONArray(content);

                // 将JSONArray转换为List<String>
                List<String> recommendations = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    recommendations.add(jsonArray.getString(i));
                }
                log.debug("获取推荐消息成功: sessionId={}, dialogueId={}, count={}", sessionId, dialogueId, recommendations.size());
                return recommendations;
            }

            log.debug("未找到推荐消息: sessionId={}, dialogueId={}", sessionId, dialogueId);
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("获取推荐消息失败: sessionId={}, dialogueId={}", sessionId, dialogueId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存Ai回复
     */
    public void saveAssistantMessage(String sessionId, Long userId, String content, Boolean isRagEnhanced) {
        saveAssistantMessage(sessionId, null, userId, content, isRagEnhanced);
    }

    /**
     * 保存Ai回复（支持dialogueId）
     */
    public String saveAssistantMessage(String sessionId, String dialogueId, Long userId, String content, Boolean isRagEnhanced) {
        try {
            ChatMessage message = new ChatMessage(sessionId, dialogueId, userId, ChatMessage.MessageType.ASSISTANT, content, isRagEnhanced);
            chatMessageRepository.save(message);
            log.debug("已保存助手回复到MongoDB: sessionId={}, dialogueId={}, isRagEnhanced={}", sessionId, dialogueId, isRagEnhanced);
            return dialogueId;
        } catch (Exception e) {
            log.error("保存助手回复到MongoDB失败: sessionId={}, dialogueId={}", sessionId, dialogueId, e);
            return null;
        }
    }

    /**
     * 获取会话历史记录
     * 因为RAG 会有提问增强 这里做职责分离
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        try {
            List<ChatMessage> chatMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            log.info("chatMessages = {}", chatMessages);
            return chatMessages;
        } catch (Exception e) {
            log.error("获取会话历史记录失败: sessionId={}", sessionId, e);
            return List.of();
        }
    }

    /**
     * 删除会话的所有消息
     */
    public void deleteSessionMessages(String sessionId) {
        try {
            chatMessageRepository.deleteBySessionId(sessionId);
            log.info("已删除会话消息: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除会话消息失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取用户的所有会话消息
     * 后续做数据分析可以用这个接口
     */
    public List<ChatMessage> getUserMessages(Long userId) {
        try {
            return chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } catch (Exception e) {
            log.error("获取用户消息失败: userId={}", userId, e);
            return List.of();
        }
    }
}

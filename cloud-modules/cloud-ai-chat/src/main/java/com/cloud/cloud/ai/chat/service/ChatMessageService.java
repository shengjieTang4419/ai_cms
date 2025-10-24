package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.ChatMessage;
import com.cloud.cloud.ai.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * 保存用户消息
     */
    public void saveUserMessage(String sessionId, Long userId, String content) {
        try {
            ChatMessage message = new ChatMessage(sessionId, userId, ChatMessage.MessageType.USER, content);
            chatMessageRepository.save(message);
            log.debug("已保存用户消息到MongoDB: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("保存用户消息到MongoDB失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 保存Ai回复
     */
    public void saveAssistantMessage(String sessionId, Long userId, String content, Boolean isRagEnhanced) {
        try {
            ChatMessage message = new ChatMessage(sessionId, userId, ChatMessage.MessageType.ASSISTANT, content, isRagEnhanced);
            chatMessageRepository.save(message);
            log.debug("已保存助手回复到MongoDB: sessionId={}, isRagEnhanced={}", sessionId, isRagEnhanced);
        } catch (Exception e) {
            log.error("保存助手回复到MongoDB失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话历史记录（纯净版本）
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

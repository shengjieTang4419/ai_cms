package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 对话服务
 * @date 2025/9/27 14:29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatDialogueService {

    private final MessageWindowChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 是否新会话（优化版本：通过数据库查询，避免从Redis获取所有历史消息）
     *
     * @param sessionId 会话ID
     * @return true表示新会话，false表示已存在
     */
    public boolean isNewSession(String sessionId) {
        try {
            // 通过数据库查询会话是否存在
            return chatSessionRepository.findBySessionId(sessionId).isEmpty();
        } catch (Exception e) {
            log.error("检查会话是否存在失败，sessionId: {}", sessionId, e);
            // 异常时降级为通过历史消息判断（保持向后兼容）
            return CollectionUtils.isEmpty(this.getConversationHistory(sessionId));
        }
    }

    /**
     * 获取会话的历史消息（从Redis记忆）
     */
    public List<Message> getConversationHistory(String sessionId) {
        try {
            return chatMemory.get(sessionId);
        } catch (Exception e) {
            log.error("获取会话历史失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 清空会话记忆
     */
    public void clearConversationMemory(String sessionId) {
        try {
            chatMemory.clear(sessionId);
            log.info("已清空会话记忆: {}", sessionId);
        } catch (Exception e) {
            log.error("清空会话记忆失败", e);
        }
    }
}

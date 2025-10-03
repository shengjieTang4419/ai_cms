package com.cloud.cloud.ai.chat.service;


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

    /**
     * 是否新会话
     *
     * @param sessionId
     * @return
     */
    public boolean isNewSession(String sessionId) {
        return CollectionUtils.isEmpty(this.getConversationHistory(sessionId));
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

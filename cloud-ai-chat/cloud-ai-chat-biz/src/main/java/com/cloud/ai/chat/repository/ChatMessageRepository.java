package com.cloud.ai.chat.repository;

import com.cloud.ai.chat.domain.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息Repository
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 聊天消息数据访问层
 * @date 2025/1/27
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 根据会话ID获取消息列表，按创建时间排序
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 根据会话ID删除所有消息
     */
    void deleteBySessionId(String sessionId);

    /**
     * 根据用户ID获取消息列表，按创建时间倒序
     */
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 获取单个会话的消息列表
     *
     * @param sessionId
     * @param dialogueId
     * @param userId
     * @return
     */
    List<ChatMessage> findBySessionIdAndDialogueIdAndUserId(String sessionId, String dialogueId, Long userId);
}

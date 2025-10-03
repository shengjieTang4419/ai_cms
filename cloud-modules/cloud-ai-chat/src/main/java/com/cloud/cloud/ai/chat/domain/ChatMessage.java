package com.cloud.cloud.ai.chat.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 聊天消息实体 - 用于存储纯净的历史记录
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 聊天消息实体
 * 参照aiChat模型 问题和回答剥离开来 一个问题 可以有多次回答
 * 也可以没有回答
 * @date 2025/1/27
 */
@Data
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    @Field("user_id")
    private Long userId;

    @Field("message_type")
    private MessageType messageType;

    @Field("content")
    private String content;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("is_rag_enhanced")
    private Boolean isRagEnhanced = false;

    public enum MessageType {
        USER, ASSISTANT
    }

    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(String sessionId, Long userId, MessageType messageType, String content) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.messageType = messageType;
        this.content = content;
    }

    public ChatMessage(String sessionId, Long userId, MessageType messageType, String content, Boolean isRagEnhanced) {
        this(sessionId, userId, messageType, content);
        this.isRagEnhanced = isRagEnhanced;
    }
}

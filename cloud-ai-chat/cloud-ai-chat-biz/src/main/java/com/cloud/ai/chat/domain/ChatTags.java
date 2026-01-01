package com.cloud.ai.chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 聊天标签表 - 记录每次聊天会话中提取的标签
 * @date 2025/10/14 14:30
 */
@Data
@Entity
@Table(name = "chat_tags")
public class ChatTags implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 会话ID
     */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /**
     * 标签名称
     */
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    /**
     * 标签频率（在该会话中出现的次数）
     */
    @Column(name = "frequency", nullable = false)
    private Integer frequency = 1;


    /**
     * 标签来源（PROFILE-来自用户画像, CHAT-来自聊天内容, FUSION-融合标签）
     */
    @Column(name = "source_type", length = 20)
    private String sourceType = "CHAT";

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

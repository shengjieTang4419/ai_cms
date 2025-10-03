package com.cloud.cloud.ai.chat.domain;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 会话级别对象
 * @date 2025/9/24 21:38
 */
@Data
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "first_query", columnDefinition = "TEXT")
    private String firstQuery;

    @Column(name = "message_count")
    private Integer messageCount = 1;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


package com.cloud.ai.chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签表
 * @date 2025/10/14 14:35
 */
@Data
@Entity
@Table(name = "user_tags")
public class UserTags implements Serializable {

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
     * 总权重
     */
    @Column(name = "total_weight", nullable = false)
    private BigDecimal totalWeight = BigDecimal.ZERO;

    /**
     * 标签来源类型（PROFILE-来自用户画像, CHAT-来自聊天内容, FUSION-融合标签）
     */
    @Column(name = "source_type", length = 20)
    private String sourceType = "CHAT";

    /**
     * 路径key
     * 生活/园艺/盆栽种植 格式
     */
    @Column(name = "path_key", length = 20)
    private String pathKey;

    /**
     * 最后更新时间
     */
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}


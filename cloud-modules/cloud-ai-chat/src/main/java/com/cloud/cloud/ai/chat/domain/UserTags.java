package com.cloud.cloud.ai.chat.domain;

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
 * @description: 用户标签表 - 用户最终的综合标签，融合了基础画像和聊天行为
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
     * 标签名称
     */
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    /**
     * 基础权重（来自用户画像）
     */
    @Column(name = "base_weight", nullable = false)
    private BigDecimal baseWeight = BigDecimal.ZERO;

    /**
     * 聊天权重（来自聊天行为）
     */
    @Column(name = "chat_weight", nullable = false)
    private BigDecimal chatWeight = BigDecimal.ZERO;

    /**
     * 融合权重（基础画像与聊天标签重叠时的额外权重）
     */
    @Column(name = "fusion_weight", nullable = false)
    private BigDecimal fusionWeight = BigDecimal.ZERO;

    /**
     * 总权重（base_weight + chat_weight + fusion_weight）
     */
    @Column(name = "total_weight", nullable = false)
    private BigDecimal totalWeight = BigDecimal.ZERO;

    /**
     * 标签来源类型（PROFILE-来自用户画像, CHAT-来自聊天内容, FUSION-融合标签）
     */
    @Column(name = "source_type", length = 20)
    private String sourceType = "CHAT";

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

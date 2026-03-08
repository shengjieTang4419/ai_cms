package com.cloud.membership.domain;

import com.cloud.memebership.domain.TagPath;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
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
     * 标签路径key
     * 人工智能/多模态/视觉理解
     */
    @Column(name = "path_key", nullable = false, length = 500)
    private String pathKey;

    /**
     * 序列化出来的TagPath
     */
    @Transient
    private TagPath tagPath;

    /**
     * 累积权重
     */
    @Column(name = "total_weight")
    private Double totalWeight;


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

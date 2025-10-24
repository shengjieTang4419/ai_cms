package com.cloud.cloud.ai.chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业信息表实体类
 * @date 2025/10/24 00:00
 */
@Data
@Entity
@Table(name = "occupations")
public class OccupationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 职业代码
     */
    @Column(name = "code", nullable = false, unique = true)
    private Integer code;

    /**
     * 职业名称
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 职业相关标签（数组）
     */
    @Column(name = "tags")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;

    /**
     * 状态：1-启用，0-禁用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


package com.cloud.membership.domain;

import com.cloud.membership.domain.enums.AgeRange;
import com.cloud.membership.domain.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象模板实体
 * 基于用户特征（性别、年龄段、职业）预设的标签权重模板
 * @date 2026/2/5 16:00
 */
@Data
@Entity
@Table(name = "stereotype_templates", indexes = {
        @Index(name = "idx_", columnList = "template_key", unique = true),
        @Index(name = "idx_gender_age_occupation", columnList = "gender, age_range, occupation_code")
})
public class StereotypeTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模板唯一标识
     * 格式: {gender}_{ageRange}_{occupationCode}
     * 例如: MALE_YOUNG_ADULT_1001
     */
    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    /**
     * 模板名称（人类可读）
     * 例如: "年轻男性程序员"
     */
    @Column(name = "template_name", length = 100)
    private String templateName;

    /**
     * 性别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    /**
     * 年龄段
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 20)
    private AgeRange ageRange;

    /**
     * 职业代码（关联 occupations 表）
     * 仅常用职业有模板，非常用职业为 null
     */
    @Column(name = "occupation_code")
    private Integer occupationCode;

    /**
     * 预设标签列表
     * 格式: ["技术/编程/Java", "生活/数码/电子产品", ...]
     */
    @Column(name = "preset_tags", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> presetTags;

    /**
     * 标签权重映射
     * 格式: {"技术/编程/Java": 0.8, "生活/数码/电子产品": 0.6, ...}
     */
    @Column(name = "tag_weights", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> tagWeights;

    /**
     * 模板描述（AI生成的刻板印象描述）
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 模板来源：SYSTEM-系统内置, AI-AI生成
     */
    @Column(name = "source", length = 20)
    private String source = "SYSTEM";

    /**
     * 状态：1-启用，0-禁用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 匹配优先级（数值越大优先级越高）
     */
    @Column(name = "priority")
    private Integer priority = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 生成模板Key
     */
    public static String generateTemplateKey(Gender gender, AgeRange ageRange, Integer occupationCode) {
        StringBuilder key = new StringBuilder();
        key.append(gender != null ? gender.name() : "ANY");
        key.append("_");
        key.append(ageRange != null ? ageRange.name() : "ANY");
        key.append("_");
        key.append(occupationCode != null ? occupationCode : "ANY");
        return key.toString();
    }
}

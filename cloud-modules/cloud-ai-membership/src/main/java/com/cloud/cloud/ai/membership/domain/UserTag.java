package com.cloud.cloud.ai.membership.domain;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签
 * @date 2025/10/13 18:20
 */
@Data
@Entity
@Table(name = "user_tags")
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String tagName;      // 标签名：编程、天气、旅游、美食等
    private Integer weight;       // 权重：表示用户对该标签的兴趣程度
    private LocalDateTime updatedAt;
}

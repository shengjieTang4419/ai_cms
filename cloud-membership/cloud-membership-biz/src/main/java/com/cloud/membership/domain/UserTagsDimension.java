package com.cloud.membership.domain;


import com.cloud.memebership.domain.TagPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签维度
 * @date 2025/10/24 17:52
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTagsDimension implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 路径唯一 key：人工智能/多模态/视觉理解
     */
    private String pathKey;

    /**
     * 原始路径
     */
    private TagPath tagPath;

    /**
     * 累积权重（可衰减）
     */
    private double totalWeight;

    /**
     * 最近一次被触达的 activeTime
     */
    private LocalDateTime lastActiveTime;

    /**
     * 标签创建时间（用于自然时间衰减）
     */
    private LocalDateTime createdTime;
}

package com.cloud.membership.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 模板标签权重DTO
 * @date 2026/2/5 16:00
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTagWeight implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签路径
     * 例如: "技术/编程/Java"
     */
    private String tagPath;

    /**
     * 初始权重 (0.0 - 1.0)
     */
    private Double weight;

    /**
     * 标签描述
     */
    private String description;
}

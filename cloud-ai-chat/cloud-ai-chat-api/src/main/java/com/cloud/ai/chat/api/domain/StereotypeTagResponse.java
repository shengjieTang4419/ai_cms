package com.cloud.ai.chat.api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签生成响应
 * @date 2026/2/5 16:30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StereotypeTagResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 生成的标签路径列表
     * 格式: ["技术/编程/Java", "生活/数码/电子产品", ...]
     */
    private List<String> tags;

    /**
     * 标签权重映射
     * 格式: {"技术/编程/Java": 0.8, ...}
     */
    private Map<String, Double> tagWeights;

    /**
     * AI生成的人物描述
     */
    private String profileDescription;

    public static StereotypeTagResponse fail(String errorMessage) {
        return StereotypeTagResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static StereotypeTagResponse success(List<String> tags, Map<String, Double> tagWeights, String description) {
        return StereotypeTagResponse.builder()
                .success(true)
                .tags(tags)
                .tagWeights(tagWeights)
                .profileDescription(description)
                .build();
    }
}

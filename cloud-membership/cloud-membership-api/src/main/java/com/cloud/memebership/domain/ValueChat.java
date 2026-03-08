package com.cloud.memebership.domain;

 
import lombok.Data;
 
import java.io.Serializable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 单次对话
 * @date 2026/1/3 11:52
 */
@Data
public class ValueChat implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String sessionId;

    private String dialogueId;

    private String query;

    /**
     * 此Tag为通过AI生成的标签
     * 一次有效对话 只产生一个标签（业务限定）
     */
    private TagPath tagPath;
}

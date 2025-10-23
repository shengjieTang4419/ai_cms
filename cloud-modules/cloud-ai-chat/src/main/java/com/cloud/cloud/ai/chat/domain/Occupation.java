package com.cloud.cloud.ai.chat.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业枚举
 * @date 2025/01/16 10:00
 */
@Getter
public enum Occupation {
    
    PROGRAMMER(1, "程序员", Arrays.asList("编程", "技术", "软件开发")),
    DESIGNER(2, "设计师", Arrays.asList("设计", "创意", "视觉")),
    TEACHER(3, "教师", Arrays.asList("教育", "学习", "知识分享")),
    DOCTOR(4, "医生", Arrays.asList("健康", "医学", "养生")),
    SALES(5, "销售", Arrays.asList("商务", "沟通", "市场")),
    FINANCE(6, "金融", Arrays.asList("金融", "投资", "理财")),
    MEDIA(7, "媒体", Arrays.asList("媒体", "传播", "内容")),
    LAWYER(8, "法律", Arrays.asList("法律", "合规", "咨询"));
    
    private final Integer code;
    private final String name;
    private final List<String> tags;
    
    Occupation(Integer code, String name, List<String> tags) {
        this.code = code;
        this.name = name;
        this.tags = tags;
    }
    
    /**
     * 根据职业代码获取职业枚举
     */
    public static Occupation fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(occupation -> occupation.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据职业代码获取标签列表
     */
    public static List<String> getTagsByCode(Integer code) {
        Occupation occupation = fromCode(code);
        return occupation != null ? occupation.getTags() : Arrays.asList("职业");
    }
}

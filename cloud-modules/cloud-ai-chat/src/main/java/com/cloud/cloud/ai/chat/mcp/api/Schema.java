package com.cloud.cloud.ai.chat.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * JSON Schema定义 - 用于工具的输入输出参数结构定义
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schema {
    
    /**
     * Schema类型：object, string, number, integer, boolean, array
     */
    private String type;
    
    /**
     * 属性描述（当type为object时）
     * key: 属性名，value: 属性的Schema定义
     */
    private Map<String, Schema> properties;
    
    /**
     * 必填字段列表
     */
    private List<String> required;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 枚举值（当type为string时）
     */
    private List<String> enumValues;
    
    /**
     * 数组元素的Schema定义（当type为array时）
     */
    private Schema items;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 最小值（数字类型）
     */
    private Number minimum;
    
    /**
     * 最大值（数字类型）
     */
    private Number maximum;
    
    /**
     * 字符串最小长度
     */
    private Integer minLength;
    
    /**
     * 字符串最大长度
     */
    private Integer maxLength;
    
    /**
     * 正则表达式模式（字符串类型）
     */
    private String pattern;
    
    /**
     * 创建一个String类型的Schema
     */
    public static Schema string(String description) {
        return Schema.builder()
                .type("string")
                .description(description)
                .build();
    }
    
    /**
     * 创建一个String类型的Schema（带枚举）
     */
    public static Schema stringEnum(String description, List<String> enumValues) {
        return Schema.builder()
                .type("string")
                .description(description)
                .enumValues(enumValues)
                .build();
    }
    
    /**
     * 创建一个Number类型的Schema
     */
    public static Schema number(String description) {
        return Schema.builder()
                .type("number")
                .description(description)
                .build();
    }
    
    /**
     * 创建一个Integer类型的Schema
     */
    public static Schema integer(String description) {
        return Schema.builder()
                .type("integer")
                .description(description)
                .build();
    }
    
    /**
     * 创建一个Boolean类型的Schema
     */
    public static Schema bool(String description) {
        return Schema.builder()
                .type("boolean")
                .description(description)
                .build();
    }
    
    /**
     * 创建一个Object类型的Schema
     */
    public static Schema object(Map<String, Schema> properties, List<String> required) {
        return Schema.builder()
                .type("object")
                .properties(properties)
                .required(required)
                .build();
    }
    
    /**
     * 创建一个Array类型的Schema
     */
    public static Schema array(Schema items, String description) {
        return Schema.builder()
                .type("array")
                .items(items)
                .description(description)
                .build();
    }
}

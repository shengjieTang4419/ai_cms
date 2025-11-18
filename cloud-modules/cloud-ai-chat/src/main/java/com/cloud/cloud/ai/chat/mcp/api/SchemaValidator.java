package com.cloud.cloud.ai.chat.mcp.api;

import com.cloud.cloud.ai.chat.mcp.api.exception.SchemaValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema验证器 - 防止AI幻觉的关键组件
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Slf4j
public class SchemaValidator {
    
    /**
     * 验证JSON数据是否符合Schema定义
     * 
     * @param schema Schema定义
     * @param data 待验证的数据
     * @throws SchemaValidationException 验证失败时抛出
     */
    public static void validate(Schema schema, JsonNode data) {
        if (schema == null) {
            return;
        }
        
        List<String> errors = new ArrayList<>();
        validateNode(schema, data, "", errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Schema验证失败:\n" + String.join("\n", errors);
            log.error(errorMessage);
            throw new SchemaValidationException(errorMessage);
        }
    }
    
    /**
     * 递归验证节点
     */
    private static void validateNode(Schema schema, JsonNode node, String path, List<String> errors) {
        if (node == null || node.isNull()) {
            errors.add(String.format("路径 %s: 值为null", path.isEmpty() ? "root" : path));
            return;
        }
        
        String type = schema.getType();
        
        switch (type) {
            case "object":
                validateObject(schema, node, path, errors);
                break;
            case "array":
                validateArray(schema, node, path, errors);
                break;
            case "string":
                validateString(schema, node, path, errors);
                break;
            case "number":
            case "integer":
                validateNumber(schema, node, path, errors);
                break;
            case "boolean":
                validateBoolean(schema, node, path, errors);
                break;
            default:
                errors.add(String.format("路径 %s: 未知的类型 %s", path, type));
        }
    }
    
    /**
     * 验证Object类型
     */
    private static void validateObject(Schema schema, JsonNode node, String path, List<String> errors) {
        if (!node.isObject()) {
            errors.add(String.format("路径 %s: 期望类型为object，实际为 %s", path, node.getNodeType()));
            return;
        }
        
        Map<String, Schema> properties = schema.getProperties();
        List<String> required = schema.getRequired();
        
        // 检查必填字段
        if (required != null) {
            for (String fieldName : required) {
                if (!node.has(fieldName)) {
                    errors.add(String.format("路径 %s: 缺少必填字段 %s", path, fieldName));
                }
            }
        }
        
        // 验证各个属性
        if (properties != null) {
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String fieldName = entry.getKey();
                Schema fieldSchema = entry.getValue();
                
                if (node.has(fieldName)) {
                    JsonNode fieldValue = node.get(fieldName);
                    String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                    validateNode(fieldSchema, fieldValue, fieldPath, errors);
                }
            }
        }
    }
    
    /**
     * 验证Array类型
     */
    private static void validateArray(Schema schema, JsonNode node, String path, List<String> errors) {
        if (!node.isArray()) {
            errors.add(String.format("路径 %s: 期望类型为array，实际为 %s", path, node.getNodeType()));
            return;
        }
        
        Schema itemSchema = schema.getItems();
        if (itemSchema != null) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode item = node.get(i);
                String itemPath = path + "[" + i + "]";
                validateNode(itemSchema, item, itemPath, errors);
            }
        }
    }
    
    /**
     * 验证String类型
     */
    private static void validateString(Schema schema, JsonNode node, String path, List<String> errors) {
        if (!node.isTextual()) {
            errors.add(String.format("路径 %s: 期望类型为string，实际为 %s", path, node.getNodeType()));
            return;
        }
        
        String value = node.asText();
        
        // 验证枚举值
        List<String> enumValues = schema.getEnumValues();
        if (enumValues != null && !enumValues.isEmpty()) {
            if (!enumValues.contains(value)) {
                errors.add(String.format("路径 %s: 值 '%s' 不在允许的枚举值中 %s", path, value, enumValues));
            }
        }
        
        // 验证最小长度
        if (schema.getMinLength() != null && value.length() < schema.getMinLength()) {
            errors.add(String.format("路径 %s: 字符串长度 %d 小于最小长度 %d", path, value.length(), schema.getMinLength()));
        }
        
        // 验证最大长度
        if (schema.getMaxLength() != null && value.length() > schema.getMaxLength()) {
            errors.add(String.format("路径 %s: 字符串长度 %d 大于最大长度 %d", path, value.length(), schema.getMaxLength()));
        }
        
        // 验证正则表达式
        if (schema.getPattern() != null && !value.matches(schema.getPattern())) {
            errors.add(String.format("路径 %s: 值 '%s' 不匹配正则表达式 %s", path, value, schema.getPattern()));
        }
    }
    
    /**
     * 验证Number类型
     */
    private static void validateNumber(Schema schema, JsonNode node, String path, List<String> errors) {
        if (!node.isNumber()) {
            errors.add(String.format("路径 %s: 期望类型为number，实际为 %s", path, node.getNodeType()));
            return;
        }
        
        if ("integer".equals(schema.getType()) && !node.isIntegralNumber()) {
            errors.add(String.format("路径 %s: 期望类型为integer，实际为浮点数", path));
            return;
        }
        
        double value = node.asDouble();
        
        // 验证最小值
        if (schema.getMinimum() != null && value < schema.getMinimum().doubleValue()) {
            errors.add(String.format("路径 %s: 值 %s 小于最小值 %s", path, value, schema.getMinimum()));
        }
        
        // 验证最大值
        if (schema.getMaximum() != null && value > schema.getMaximum().doubleValue()) {
            errors.add(String.format("路径 %s: 值 %s 大于最大值 %s", path, value, schema.getMaximum()));
        }
    }
    
    /**
     * 验证Boolean类型
     */
    private static void validateBoolean(Schema schema, JsonNode node, String path, List<String> errors) {
        if (!node.isBoolean()) {
            errors.add(String.format("路径 %s: 期望类型为boolean，实际为 %s", path, node.getNodeType()));
        }
    }
}

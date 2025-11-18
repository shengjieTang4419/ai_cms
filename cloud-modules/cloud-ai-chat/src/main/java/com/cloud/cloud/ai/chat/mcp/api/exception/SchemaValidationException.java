package com.cloud.cloud.ai.chat.mcp.api.exception;

/**
 * Schema验证异常
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
public class SchemaValidationException extends McpToolException {
    
    public SchemaValidationException(String message) {
        super("SCHEMA_VALIDATION_ERROR", message);
    }
    
    public SchemaValidationException(String field, String expectedType, String actualValue) {
        super("SCHEMA_VALIDATION_ERROR", 
              String.format("参数验证失败 - 字段: %s, 期望类型: %s, 实际值: %s", 
                          field, expectedType, actualValue));
    }
}

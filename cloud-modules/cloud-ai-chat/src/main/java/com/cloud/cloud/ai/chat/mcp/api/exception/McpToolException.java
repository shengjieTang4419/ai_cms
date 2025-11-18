package com.cloud.cloud.ai.chat.mcp.api.exception;

/**
 * MCP工具异常基类
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
public class McpToolException extends RuntimeException {
    
    private final String errorCode;
    
    public McpToolException(String message) {
        super(message);
        this.errorCode = "MCP_ERROR";
    }
    
    public McpToolException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public McpToolException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MCP_ERROR";
    }
    
    public McpToolException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

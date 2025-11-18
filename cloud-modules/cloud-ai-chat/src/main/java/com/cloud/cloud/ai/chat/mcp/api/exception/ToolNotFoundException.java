package com.cloud.cloud.ai.chat.mcp.api.exception;

/**
 * 工具不存在异常
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
public class ToolNotFoundException extends McpToolException {
    
    public ToolNotFoundException(String toolName) {
        super("TOOL_NOT_FOUND", "工具不存在: " + toolName);
    }
}

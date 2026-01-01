package com.cloud.ai.chat.config;


import com.cloud.ai.chat.mcp.adapter.McpToolAdapter;
import com.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.ai.chat.mcp.api.McpToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 2.0.0
 * @description: MCP 客户端配置 - 统一使用新的MCP工具系统
 * @date 2025/10/8 11:34
 */
@Configuration
@Slf4j
public class MCPConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private McpToolRegistry mcpToolRegistry;

    /**
     * 配置ToolCallbackProvider - 使用新的MCP工具系统
     * <p>
     * 所有工具都通过McpTool接口实现，通过McpToolAdapter适配到Spring AI
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        log.info("开始配置MCP工具系统...");

        // 将所有MCP工具转换为Spring AI的ToolCallback
        List<ToolCallback> mcpToolCallbacks = convertMcpToolsToCallbacks();

        log.info("✅ MCP工具系统配置完成，共注册 {} 个工具", mcpToolCallbacks.size());

        return () -> mcpToolCallbacks.toArray(new ToolCallback[0]);
    }

    /**
     * 将所有MCP工具转换为Spring AI的ToolCallback
     */
    private List<ToolCallback> convertMcpToolsToCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (McpTool mcpTool : mcpToolRegistry.getEnabledTools()) {
            try {
                ToolCallback callback = new McpToolAdapter(mcpTool, objectMapper);
                callbacks.add(callback);
                log.info("  ✓ {} (分类: {}, 版本: {})",
                        mcpTool.getName(),
                        mcpTool.getCategory(),
                        mcpTool.getVersion());
            } catch (Exception e) {
                log.error("  ✗ 适配MCP工具失败: {}", mcpTool.getName(), e);
            }
        }
        return callbacks;
    }
}

package com.cloud.cloud.ai.chat.mcp.api;

import com.cloud.cloud.ai.chat.mcp.api.exception.McpToolException;
import com.cloud.cloud.ai.chat.mcp.api.exception.SchemaValidationException;
import com.cloud.cloud.ai.chat.mcp.api.exception.ToolNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具执行器 - 统一调度和执行工具
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpExecutor {

    private final McpToolRegistry registry;
    private final ObjectMapper objectMapper;

    /**
     * 执行工具调用
     *
     * @param toolName  工具名称
     * @param arguments 参数（JsonNode格式）
     * @return 执行结果
     * @throws ToolNotFoundException     工具不存在
     * @throws SchemaValidationException 参数或结果验证失败
     * @throws McpToolException          其他执行异常
     */
    public Object executeToolCall(String toolName, JsonNode arguments) {
        log.info("执行工具调用: {} with arguments: {}", toolName, arguments);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取工具
            McpTool tool = registry.get(toolName);
            if (tool == null) {
                throw new ToolNotFoundException(toolName);
            }

            // 检查工具是否可用
            if (!tool.isEnabled()) {
                throw new McpToolException("TOOL_DISABLED", "工具已禁用: " + toolName);
            }

            log.debug("找到工具: {} ({})", toolName, tool.getDescription());

            // 2. 校验输入参数（防止AI幻觉）
            Schema inputSchema = tool.getInputSchema();
            if (inputSchema != null) {
                log.debug("验证输入参数 Schema");
                SchemaValidator.validate(inputSchema, arguments);
            }

            // 3. 执行工具
            log.debug("开始执行工具: {}", toolName);
            Object result = tool.execute(arguments);

            // 4. 校验输出结果（确保格式正确）
            Schema outputSchema = tool.getOutputSchema();
            if (outputSchema != null && result != null) {
                log.debug("验证输出结果 Schema");
                JsonNode resultNode = objectMapper.valueToTree(result);
                SchemaValidator.validate(outputSchema, resultNode);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("工具执行成功: {}, 耗时: {}ms", toolName, duration);

            return result;

        } catch (ToolNotFoundException | SchemaValidationException e) {
            log.error("工具执行失败: {}, 错误: {}", toolName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("工具执行异常: {}", toolName, e);
            throw new McpToolException("EXECUTION_ERROR", "工具执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行工具调用（Map参数）
     *
     * @param toolName  工具名称
     * @param arguments 参数Map
     * @return 执行结果
     */
    public Object executeToolCall(String toolName, Map<String, Object> arguments) {
        JsonNode argsNode = objectMapper.valueToTree(arguments);
        return executeToolCall(toolName, argsNode);
    }

    /**
     * 批量执行工具调用
     *
     * @param toolCalls 工具调用列表
     * @return 执行结果列表
     */
    public Map<String, Object> executeBatch(java.util.List<ToolCall> toolCalls) {
        Map<String, Object> results = new HashMap<>();

        for (ToolCall call : toolCalls) {
            try {
                Object result = executeToolCall(call.getToolName(), call.getArguments());
                results.put(call.getToolName(), result);
            } catch (Exception e) {
                log.error("批量执行工具失败: {}", call.getToolName(), e);
                results.put(call.getToolName(), Map.of(
                        "error", true,
                        "message", e.getMessage()
                ));
            }
        }

        return results;
    }

    /**
     * 工具调用请求对象
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolCall {
        private String toolName;
        private JsonNode arguments;
    }
}

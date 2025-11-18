package com.cloud.cloud.ai.chat.mcp.adapter;

import com.cloud.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.cloud.ai.chat.mcp.api.Schema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具适配器 - 将McpTool适配为Spring AI的ToolCallback
 * <p>
 * 这个适配器让新的MCP工具系统能够无缝集成到Spring AI的ChatClient中
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Slf4j
public class McpToolAdapter implements ToolCallback {

    private final McpTool mcpTool;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;

    public McpToolAdapter(McpTool mcpTool, ObjectMapper objectMapper) {
        this.mcpTool = mcpTool;
        this.objectMapper = objectMapper;
        this.toolDefinition = buildToolDefinition();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String functionArguments) {
        try {
            log.debug("适配器执行工具: {}, 输入: {}", mcpTool.getName(), functionArguments);

            // 将JSON字符串解析为JsonNode
            JsonNode inputNode = objectMapper.readTree(functionArguments);

            // 执行MCP工具
            Object result = mcpTool.execute(inputNode);

            // 将结果转换为JSON字符串
            if (result instanceof String) {
                return (String) result;
            } else {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            log.error("工具执行失败: {}", mcpTool.getName(), e);
            return String.format("工具执行失败: %s", e.getMessage());
        }
    }

    /**
     * 构建ToolDefinition
     */
    private ToolDefinition buildToolDefinition() {
        try {
            String name = mcpTool.getName();
            String description = mcpTool.getDescription();
            String inputSchemaJson = getInputSchemaJson();

            return ToolDefinition.builder()
                    .name(name)
                    .description(description)
                    .inputSchema(inputSchemaJson)
                    .build();
        } catch (Exception e) {
            log.error("构建ToolDefinition失败: {}", mcpTool.getName(), e);
            throw new RuntimeException("构建工具定义失败", e);
        }
    }

    /**
     * 获取输入Schema的JSON字符串
     */
    private String getInputSchemaJson() {
        try {
            Schema inputSchema = mcpTool.getInputSchema();
            if (inputSchema == null) {
                return "{}";
            }

            Map<String, Object> jsonSchema = convertSchemaToMap(inputSchema);
            return objectMapper.writeValueAsString(jsonSchema);
        } catch (Exception e) {
            log.error("转换Schema失败: {}", mcpTool.getName(), e);
            return "{}";
        }
    }

    /**
     * 将MCP的Schema转换为Spring AI需要的Map格式
     */
    private Map<String, Object> convertSchemaToMap(Schema schema) {
        Map<String, Object> map = new HashMap<>();

        if (schema.getType() != null) {
            map.put("type", schema.getType());
        }

        if (schema.getDescription() != null) {
            map.put("description", schema.getDescription());
        }

        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new HashMap<>();
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                properties.put(entry.getKey(), convertSchemaToMap(entry.getValue()));
            }
            map.put("properties", properties);
        }

        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            map.put("required", schema.getRequired());
        }

        if (schema.getEnumValues() != null && !schema.getEnumValues().isEmpty()) {
            map.put("enum", schema.getEnumValues());
        }

        if (schema.getItems() != null) {
            map.put("items", convertSchemaToMap(schema.getItems()));
        }

        if (schema.getDefaultValue() != null) {
            map.put("default", schema.getDefaultValue());
        }

        if (schema.getMinimum() != null) {
            map.put("minimum", schema.getMinimum());
        }

        if (schema.getMaximum() != null) {
            map.put("maximum", schema.getMaximum());
        }

        if (schema.getMinLength() != null) {
            map.put("minLength", schema.getMinLength());
        }

        if (schema.getMaxLength() != null) {
            map.put("maxLength", schema.getMaxLength());
        }

        if (schema.getPattern() != null) {
            map.put("pattern", schema.getPattern());
        }

        return map;
    }
}

package com.cloud.cloud.ai.chat.mcp.tools.basic;

import com.cloud.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.cloud.ai.chat.mcp.api.Schema;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ping测试工具 - 用于测试MCP系统是否正常工作
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Component
@Slf4j
public class PingMcpTool implements McpTool {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "测试工具，用于验证MCP系统是否正常工作。返回当前时间和接收到的消息。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("message", Schema.string("测试消息"));
        // message是可选的
        return Schema.object(properties, List.of());
    }

    @Override
    public Schema getOutputSchema() {
        return Schema.string("Pong响应，包含时间戳和消息");
    }

    @Override
    public boolean match(String query) {
        if (query == null) {
            return false;
        }
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("ping") ||
                lowerQuery.contains("测试") ||
                lowerQuery.contains("test");
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        String message = "Hello";

        if (input != null && input.has("message")) {
            message = input.get("message").asText();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("Ping工具被调用: message={}, timestamp={}", message, timestamp);

        return String.format("Pong! 收到消息: %s, 服务器时间: %s", message, timestamp);
    }

    @Override
    public String getCategory() {
        return "basic";
    }
}

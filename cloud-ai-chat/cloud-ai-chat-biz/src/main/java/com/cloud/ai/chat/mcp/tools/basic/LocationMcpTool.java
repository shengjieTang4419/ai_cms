package com.cloud.ai.chat.mcp.tools.basic;


import com.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.ai.chat.mcp.api.Schema;
import com.cloud.ai.chat.mcp.service.LocationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定位工具 - 基于MCP架构的实现
 * 提供IP定位和坐标定位功能
 *
 * @author shengjie.tang
 * @version 2.0.0
 * @date 2025/11/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationMcpTool implements McpTool {

    private final LocationService locationService;

    @Override
    public String getName() {
        return "get_location_by_ip";
    }

    @Override
    public String getDescription() {
        return "根据IP地址获取位置信息，返回省份、城市等信息。精度为城市级别。" +
                "如果不提供IP参数，系统会自动使用请求来源的IP地址进行定位。这是获取用户当前位置的主要方法。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("ip", Schema.string("IP地址，例如：114.114.114.114。如果不提供或为空，将自动使用请求来源的IP地址"));
        return Schema.object(properties, List.of());
    }

    @Override
    public Schema getOutputSchema() {
        return Schema.string("位置信息，包含省份、城市、区域编码等");
    }

    @Override
    public boolean match(String query) {
        if (query == null) {
            return false;
        }
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("定位") ||
                lowerQuery.contains("位置") ||
                lowerQuery.contains("在哪") ||
                lowerQuery.contains("location");
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        final String ip;
        if (input != null && input.has("ip") && !input.get("ip").isNull()) {
            ip = input.get("ip").asText();
        } else {
            ip = null;
        }

        log.info("开始获取IP定位信息，IP地址：{}", ip != null ? ip : "使用请求IP");

        try {
            return locationService.getLocationByIp(ip)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSubscribe(s -> log.debug("订阅IP定位服务"))
                    .map(response -> {
                        log.info("IP定位API调用成功：{}", response);

                        // 检查API响应状态
                        if (!"1".equals(response.getStatus())) {
                            log.error("IP定位API返回错误状态：{}", response.getInfo());
                            return String.format("定位服务暂时不可用：%s。请稍后重试。", response.getInfo());
                        }

                        // 构建返回信息
                        StringBuilder result = new StringBuilder();
                        if (response.getProvince() != null) {
                            result.append("省份：").append(response.getProvince());
                        }
                        if (response.getCity() != null) {
                            if (!result.isEmpty()) result.append("，");
                            result.append("城市：").append(response.getCity());
                        }
                        if (response.getAdcode() != null) {
                            if (!result.isEmpty()) result.append("，");
                            result.append("区域编码：").append(response.getAdcode());
                        }
                        if (ip != null && !ip.isEmpty()) {
                            result.append("（IP：").append(ip).append("）");
                        }

                        return !result.isEmpty() ? result.toString() : "未获取到位置信息";
                    })
                    .doOnError(error -> log.error("IP定位查询失败：{}", error.getMessage(), error))
                    .onErrorReturn("抱歉，IP定位服务暂时出现故障，请稍后重试。")
                    .block();
        } catch (Exception e) {
            log.error("IP定位查询异常：{}", e.getMessage(), e);
            return "IP定位服务暂时不可用，请稍后重试。";
        }
    }

    @Override
    public String getCategory() {
        return "basic";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }
}

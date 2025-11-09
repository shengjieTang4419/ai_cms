package com.cloud.cloud.ai.chat.config;

import com.cloud.cloud.ai.chat.mcp.service.tool.LocationTools;
import com.cloud.cloud.ai.chat.mcp.service.tool.PersonalizedRecommendationTools;
import com.cloud.cloud.ai.chat.mcp.service.tool.RoutePlanningTools;
import com.cloud.cloud.ai.chat.mcp.service.tool.WeatherTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: MCP 客户端配置
 * @date 2025/10/8 11:34
 */
@Configuration
@Slf4j
public class MCPConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(WeatherTools weatherTools, 
                                                     PersonalizedRecommendationTools recommendationTools,
                                                     LocationTools locationTools,
                                                     RoutePlanningTools routePlanningTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, recommendationTools, locationTools, routePlanningTools)
                .build();
    }
}

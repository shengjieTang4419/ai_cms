package com.cloud.ai.chat.mcp.tools.basic;


import com.cloud.ai.chat.domain.WeatherInfo;
import com.cloud.ai.chat.domain.WeatherResponse;
import com.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.ai.chat.mcp.api.Schema;
import com.cloud.ai.chat.mcp.service.LocationService;
import com.cloud.ai.chat.mcp.service.WeatherService;
import com.cloud.ai.chat.service.impl.CityInfoService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气查询工具 - 基于MCP架构的实现
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherMcpTool implements McpTool {

    private final WeatherService weatherService;
    private final LocationService locationService;
    private final CityInfoService cityInfoService;

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "获取指定城市或当前位置的实时天气信息。如果提供了城市名称参数（如：北京、上海），则查询该城市的天气；" +
                "如果没有提供城市名称，则自动通过IP定位获取用户所在城市的天气。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("city", Schema.string("城市名称，例如：北京、上海、深圳等。如果不提供此参数，系统将自动通过IP定位获取用户所在城市"));
        // city字段是可选的
        return Schema.object(properties, List.of());
    }

    @Override
    public Schema getOutputSchema() {
        // 返回字符串类型的天气描述
        return Schema.string("格式化后的天气信息，包含城市、天气状况、温度、风向、风力、湿度、更新时间等");
    }

    @Override
    public boolean match(String query) {
        if (query == null) {
            return false;
        }
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("天气") ||
                lowerQuery.contains("weather") ||
                lowerQuery.contains("温度") ||
                lowerQuery.contains("气温");
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        String cityName = null;
        if (input != null && input.has("city")) {
            cityName = input.get("city").asText();
        }

        String cityCode = getCityCode(cityName);
        // 使用指定的城市编码查询天气
        try {
            WeatherResponse weatherResponse = weatherService.getWeather(cityCode)
                    .timeout(Duration.ofSeconds(10))
                    .doOnError(error -> log.error("天气查询失败：{}", error.getMessage(), error))
                    .block();
            if (weatherResponse == null) {
                return "天气查询服务返回为空，请稍后重试。";
            }
            return formatWeatherResponse(weatherResponse);
        } catch (Exception e) {
            log.error("天气查询异常：{}", e.getMessage(), e);
            return "天气查询服务暂时不可用，请稍后重试。";
        }
    }


    private String getCityCode(String cityName) {
        String cityCode;
        // 如果用户指定了城市名称，通过城市名称获取城市编码
        if (StringUtils.hasText(cityName)) {
            log.info("用户指定了城市：{}，开始查询该城市的天气", cityName);
            cityCode = cityInfoService.getCityCode(cityName.trim());

            if (cityCode == null || cityCode.isEmpty()) {
                log.warn("未找到城市：{}", cityName);
                return String.format("抱歉，未找到城市\"%s\"的信息，请检查城市名称是否正确。", cityName);
            }
            log.info("城市名称匹配成功：{} -> 城市编码：{}", cityName, cityCode);
        } else {
            // todo 后续根据前端获取地址信息 而非服务端ip
            log.info("用户未指定城市，将通过IP定位获取城市信息");
            cityCode = locationService.getCityCodeByIp(null);
            if (cityCode == null || cityCode.isEmpty()) {
                log.warn("无法通过IP定位获取城市编码");
                return "抱歉，无法通过IP定位获取城市信息，请稍后重试。";
            }
        }
        return cityCode;
    }

    @Override
    public String getCategory() {
        return "basic";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    /**
     * 格式化天气响应结果
     */
    private String formatWeatherResponse(WeatherResponse weatherResponse) {
        log.info("天气API调用成功：{}", weatherResponse);

        // 检查API响应状态
        if (!"1".equals(weatherResponse.getStatus())) {
            log.error("天气API返回错误状态：{}", weatherResponse.getInfo());
            return String.format("天气服务暂时不可用：%s。请稍后重试。", weatherResponse.getInfo());
        }

        WeatherInfo[] lives = weatherResponse.getLives();
        if (lives == null || lives.length == 0) {
            log.warn("天气API返回数据异常：无天气信息");
            return "未获取到天气信息";
        }

        WeatherInfo weather = lives[0];
        return String.format("%s天气：%s，温度：%s℃，风向：%s，风力：%s，湿度：%s%%，更新时间：%s",
                weather.getCity(), weather.getWeather(), weather.getTemperature(),
                weather.getWinddirection(), weather.getWindpower(),
                weather.getHumidity(), weather.getReporttime());
    }
}

package com.cloud.cloud.ai.chat.mcp.service.tool;

import com.cloud.cloud.ai.chat.domain.WeatherInfo;
import com.cloud.cloud.ai.chat.mcp.service.LocationService;
import com.cloud.cloud.ai.chat.mcp.service.WeatherService;
import com.cloud.cloud.ai.chat.service.CityInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 天气工具 - 获取指定城市或当前位置的实时天气信息
 * @date 2025/10/8 15:48
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherTools {

    private final WeatherService weatherService;
    private final LocationService locationService;
    private final CityInfoService cityInfoService;

    @Tool(name = "get_weather", description = "获取指定城市或当前位置的实时天气信息。如果提供了城市名称参数（如：北京、上海），则查询该城市的天气；如果没有提供城市名称，则自动通过IP定位获取用户所在城市的天气。")
    public String getWeather(@ToolParam(description = "城市名称，例如：北京、上海、深圳等。如果不提供此参数，系统将自动通过IP定位获取用户所在城市", required = false) String cityName) {
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
            // 如果没有指定城市，通过IP定位获取城市编码
            log.info("用户未指定城市，将通过IP定位获取城市信息");
            cityCode = locationService.getCityCodeByIp(null);  // null表示使用请求IP
            
            if (cityCode == null || cityCode.isEmpty()) {
                log.warn("无法通过IP定位获取城市编码");
                return "抱歉，无法通过IP定位获取城市信息，请稍后重试。";
            }
        }

        // 使用指定的城市编码查询天气
        try {
            return weatherService.getWeather(cityCode)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .map(weatherResponse -> formatWeatherResponse(weatherResponse))
                    .onErrorResume(error -> {
                        log.error("天气查询失败：{}", error.getMessage(), error);
                        return Mono.just("抱歉，天气查询服务暂时出现故障，请稍后重试。");
                    })
                    .block();  // 阻塞等待结果
        } catch (Exception e) {
            log.error("天气查询异常：{}", e.getMessage(), e);
            return "天气查询服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 格式化天气响应结果
     */
    private String formatWeatherResponse(com.cloud.cloud.ai.chat.domain.WeatherResponse weatherResponse) {
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

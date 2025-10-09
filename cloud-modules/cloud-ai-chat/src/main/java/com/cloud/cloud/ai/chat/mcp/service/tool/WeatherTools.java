package com.cloud.cloud.ai.chat.mcp.service.tool;


import com.cloud.cloud.ai.chat.domain.CityInfo;
import com.cloud.cloud.ai.chat.domain.WeatherInfo;
import com.cloud.cloud.ai.chat.mcp.service.WeatherService;
import com.cloud.cloud.ai.chat.service.CityInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/10/8 15:48
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherTools {

    private final WeatherService weatherService;
    private final CityInfoService cityInfoService;

    @Tool(name = "get_weather", description = "获取指定城市的实时天气信息")
    public String getWeather(@ToolParam(description = "城市名称，如：上海、北京、浦东新区、上海市浦东新区等") String cityName) {
        log.info("开始获取天气信息，城市名称：{}", cityName);

        try {
            // 1. 智能匹配城市编码
            CityInfo cityInfo = cityInfoService.getCityInfoByName(cityName);
            if (cityInfo == null) {
                log.warn("未找到匹配的城市：{}", cityName);
                return String.format("抱歉，无法识别您输入的城市名称：%s。请提供更准确的城市名称，如：上海、北京、浦东新区等。", cityName);
            }

            String cityCode = cityInfo.getAmapCityCode();
            String fullName = cityInfo.getFullName();
            log.info("城市匹配成功：{} -> {} ({})", cityName, cityCode, fullName);

            // 2. 调用天气服务获取天气信息
            return weatherService.getWeather(cityCode)
                    .timeout(java.time.Duration.ofSeconds(10))  // 设置10秒超时
                    .doOnSubscribe(s -> log.debug("订阅天气服务"))
                    .map(weatherResponse -> {
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
                    })
                    .doOnError(error -> {
                        log.error("天气查询失败：{}", error.getMessage(), error);
                    })
                    .onErrorReturn("抱歉，天气查询服务暂时出现故障，请稍后重试。")
                    .block();  // 阻塞等待结果
        } catch (Exception e) {
            log.error("天气查询异常：{}", e.getMessage(), e);
            return "天气查询服务暂时不可用，请稍后重试。";
        }
    }
}

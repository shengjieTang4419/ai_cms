package com.cloud.cloud.ai.chat.mcp.service;


import com.cloud.cloud.ai.chat.domain.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/10/6 09:53
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    // 使用 Web服务 Key（后端 HTTP API 调用）
    @Value("${map.web-service-key}")
    private String apiKey;

    //Spring name match 注入问题 匹配MapConfig的amapWebClient
    private final WebClient amapWebClient;

    public Mono<WeatherResponse> getWeather(String cityCode) {
        return amapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/weather/weatherInfo")
                        .queryParam("key", apiKey)
                        .queryParam("city", cityCode)
                        .queryParam("extensions", "base")
                        .queryParam("output", "JSON")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .doOnSuccess(response -> log.info("获取天气信息成功: {}", response))
                .doOnError(error -> log.error("获取天气信息失败", error));
    }
}

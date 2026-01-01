package com.cloud.ai.chat.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/10/8 11:34
 */
@Configuration
@ConfigurationProperties(prefix = "map")
@Data
public class MapConfig {

    private String apiKey;
    private String baseUrl = "https://restapi.amap.com";

    @Bean
    public WebClient amapWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}

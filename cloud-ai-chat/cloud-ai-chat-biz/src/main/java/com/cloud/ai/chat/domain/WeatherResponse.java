package com.cloud.ai.chat.domain;


import lombok.Data;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/10/8 15:53
 */
@Data
public class WeatherResponse {

    private String status;
    private String info;
    private WeatherInfo[] lives;
}

package com.cloud.cloud.ai.chat.domain;


import lombok.Data;

import java.io.Serializable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 天气信息
 * @date 2025/10/8 15:52
 */
@Data
public class WeatherInfo implements Serializable {

    //省份
    private String province;
    //城市
    private String city;
    //区域编码
    private String adcode;
    //天气现象（汉字描述）
    private String weather;
    //实时气温，单位：摄氏度
    private String temperature;
    //风向描述
    private String winddirection;
    //风力级别，单位：级
    private String windpower;
    //空气湿度
    private String humidity;
    // 数据发布的时间
    private String reporttime;
}

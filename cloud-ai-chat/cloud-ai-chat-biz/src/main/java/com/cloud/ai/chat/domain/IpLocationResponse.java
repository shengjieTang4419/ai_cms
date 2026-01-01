package com.cloud.ai.chat.domain;

import lombok.Data;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: IP定位响应
 * @date 2025/10/8 16:00
 */
@Data
public class IpLocationResponse {

    private String status;
    private String info;
    private String infocode;
    private String province;  // 省份
    private String city;      // 城市
    private String adcode;    // 区域编码
    private String rectangle; // 所在城市矩形区域范围
}


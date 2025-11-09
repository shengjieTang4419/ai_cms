package com.cloud.cloud.ai.chat.domain;

import lombok.Data;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 逆地理编码响应
 * @date 2025/10/8 16:00
 */
@Data
public class RegeoResponse {

    private String status;
    private String info;
    private String infocode;
    private Regeocode regeocode;

    @Data
    public static class Regeocode {
        private String formatted_address;  // 格式化地址
        private AddressComponent addressComponent;
    }

    @Data
    public static class AddressComponent {
        private String country;      // 国家
        private String province;     // 省份
        private String city;         // 城市
        private String district;     // 区县
        private String township;     // 街道
        private String adcode;       // 区域编码
        private String street;       // 街道名称
        private String streetNumber; // 街道门牌号
    }
}


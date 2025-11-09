package com.cloud.cloud.ai.chat.enums;

import lombok.Getter;

/**
 * 路线规划类型枚举
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 定义路线规划的类型（驾车、步行、骑行）
 * @date 2025/01/17
 */
@Getter
public enum RouteType {
    /**
     * 驾车路线规划
     */
    DRIVING("driving", "/v5/direction/driving", "驾车"),

    /**
     * 步行路线规划
     */
    WALKING("walking", "/v5/direction/walking", "步行"),

    /**
     * 骑行路线规划
     */
    BICYCLING("bicycling", "/v5/direction/bicycling", "骑行");

    /**
     * 路线类型代码
     */
    private final String code;

    /**
     * API路径
     */
    private final String apiPath;

    /**
     * 显示名称
     */
    private final String displayName;

    RouteType(String code, String apiPath, String displayName) {
        this.code = code;
        this.apiPath = apiPath;
        this.displayName = displayName;
    }
}


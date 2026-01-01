package com.cloud.ai.chat.domain;

import lombok.Data;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 路线规划响应 - 高德路径规划2.0 API响应模型
 * @date 2025/01/17
 */
@Data
public class RouteResponse {

    private String status;
    private String info;
    private String infocode;
    private Route route;

    @Data
    public static class Route {
        private String origin;           // 起点坐标
        private String destination;       // 终点坐标
        private String taxi_cost;         // 预估出租车费用（元）
        private List<Path> paths;         // 路线方案列表

        @Data
        public static class Path {
            private String distance;      // 路线距离（米）
            private String duration;      // 路线耗时（秒）
            private String strategy;      // 策略
            private String tolls;         // 收费（元）
            private String toll_distance; // 收费路段距离（米）
            private String restriction;   // 限行状态：0-不限行，1-限行
            private String traffic_lights; // 红绿灯个数
            private List<Step> steps;     // 路线步骤列表
            private String polyline;      // 路线坐标点串

            @Data
            public static class Step {
                private String instruction;    // 行走指示
                private String road;           // 道路名称
                private String distance;       // 此段距离（米）
                private String duration;       // 此段耗时（秒）
                private String polyline;       // 此段坐标点串
                private String action;         // 导航主要动作指令
                private String assistant_action; // 导航辅助动作指令
            }
        }
    }
}


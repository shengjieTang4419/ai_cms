package com.cloud.ai.chat.mcp.service;


import com.cloud.ai.chat.domain.RouteResponse;
import com.cloud.ai.chat.enums.RouteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 路线规划服务 - 调用高德路径规划2.0 API
 * @date 2025/01/17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePlanningService {

    // 使用 Web服务 Key（后端 HTTP API 调用）
    @Value("${map.web-service-key}")
    private String apiKey;

    //Spring name match 注入问题 匹配MapConfig的amapWebClient
    private final WebClient amapWebClient;

    /**
     * 路线规划（统一方法）
     * 根据路线类型、起终点坐标检索符合条件的路线规划方案
     *
     * @param routeType   路线类型（驾车、步行、骑行）
     * @param origin      起点坐标，格式：经度,纬度 例如：116.434307,39.90909
     * @param destination 终点坐标，格式：经度,纬度 例如：116.434446,39.90816
     * @return 路线规划响应
     */
    public Mono<RouteResponse> planRoute(RouteType routeType, String origin, String destination) {
        return amapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(routeType.getApiPath())
                        .queryParam("key", apiKey)
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("output", "JSON")
                        .build())
                .retrieve()
                .bodyToMono(RouteResponse.class)
                .doOnSuccess(response -> log.info("{}路线规划成功: origin={}, destination={}",
                        routeType.getDisplayName(), origin, destination))
                .doOnError(error -> log.error("{}路线规划失败: origin={}, destination={}",
                        routeType.getDisplayName(), origin, destination, error));
    }

    /**
     * 驾车路线规划（兼容旧方法）
     *
     * @deprecated 使用 planRoute(RouteType.DRIVING, origin, destination) 替代
     */
    @Deprecated
    public Mono<RouteResponse> planDrivingRoute(String origin, String destination) {
        return planRoute(RouteType.DRIVING, origin, destination);
    }

    /**
     * 步行路线规划（兼容旧方法）
     *
     * @deprecated 使用 planRoute(RouteType.WALKING, origin, destination) 替代
     */
    @Deprecated
    public Mono<RouteResponse> planWalkingRoute(String origin, String destination) {
        return planRoute(RouteType.WALKING, origin, destination);
    }

    /**
     * 骑行路线规划（兼容旧方法）
     *
     * @deprecated 使用 planRoute(RouteType.BICYCLING, origin, destination) 替代
     */
    @Deprecated
    public Mono<RouteResponse> planBicyclingRoute(String origin, String destination) {
        return planRoute(RouteType.BICYCLING, origin, destination);
    }
}


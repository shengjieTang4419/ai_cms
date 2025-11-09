package com.cloud.cloud.ai.chat.mcp.service.tool;

import com.cloud.cloud.ai.chat.domain.RouteResponse;
import com.cloud.cloud.ai.chat.enums.RouteType;
import com.cloud.cloud.ai.chat.mcp.service.LocationService;
import com.cloud.cloud.ai.chat.mcp.service.RoutePlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 路线规划工具 - 提供驾车、步行、骑行路线规划功能
 * @date 2025/01/17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoutePlanningTools {

    private final RoutePlanningService routePlanningService;
    private final LocationService locationService;

    /**
     * 路线规划（统一方法）
     * 支持驾车、步行、骑行三种路线规划方式
     * 出发地和目的地都可以是地址或坐标（经纬度，格式：经度,纬度）
     * 系统会自动进行地理编码获取坐标
     *
     * @param routeType   路线类型：driving（驾车）、walking（步行）、bicycling（骑行）
     * @param origin      出发地，可以是地址（如"北京市天安门"）或坐标（如"116.434307,39.90909"）
     * @param destination 目的地，可以是地址（如"北京市故宫"）或坐标（如"116.434446,39.90816"）
     * @return 路线规划结果描述
     */
    @Tool(name = "plan_route", description = "规划路线。支持三种路线类型：driving（驾车）、walking（步行）、bicycling（骑行）。出发地和目的地都可以是地址（如\"北京市天安门\"）或坐标（格式：经度,纬度，如\"116.434307,39.90909\"）。系统会自动将地址转换为坐标后再规划路线。返回路线距离、耗时等信息。驾车路线还会返回费用信息。")
    public String planRoute(
            @ToolParam(description = "路线类型：driving（驾车）、walking（步行）、bicycling（骑行）") String routeType,
            @ToolParam(description = "出发地，可以是地址（如\"北京市天安门\"）或坐标（格式：经度,纬度，如\"116.434307,39.90909\"）") String origin,
            @ToolParam(description = "目的地，可以是地址（如\"北京市故宫\"）或坐标（格式：经度,纬度，如\"116.434446,39.90816\"）") String destination) {

        // 解析路线类型
        RouteType type;
        try {
            type = RouteType.valueOf(routeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的路线类型：{}，使用默认类型：驾车", routeType);
            return String.format("无效的路线类型：%s。支持的路线类型：driving（驾车）、walking（步行）、bicycling（骑行）", routeType);
        }

        log.info("开始规划{}路线，出发地：{}，目的地：{}", type.getDisplayName(), origin, destination);

        try {
            // 获取出发地坐标（支持地址或坐标格式）
            String originCoord = locationService.getCoordinate(origin);
            if (originCoord == null) {
                return "无法获取出发地坐标，请检查地址是否正确（例如：北京市天安门）或坐标格式是否正确（格式：经度,纬度）";
            }

            // 获取目的地坐标（支持地址或坐标格式）
            String destinationCoord = locationService.getCoordinate(destination);
            if (destinationCoord == null) {
                return "无法获取目的地坐标，请检查地址是否正确（例如：北京市故宫）或坐标格式是否正确（格式：经度,纬度）";
            }

            // 调用路线规划服务
            RouteResponse response = routePlanningService.planRoute(type, originCoord, destinationCoord)
                    .timeout(Duration.ofSeconds(15))
                    .doOnSubscribe(s -> log.debug("订阅{}路线规划服务", type.getDisplayName()))
                    .block();

            if (response == null) {
                return "路线规划服务返回为空，请稍后重试";
            }

            // 检查API响应状态
            if (!"1".equals(response.getStatus())) {
                log.error("路线规划API返回错误状态：{}", response.getInfo());
                return String.format("路线规划失败：%s。请检查起点和终点是否正确。", response.getInfo());
            }

            if (response.getRoute() == null || response.getRoute().getPaths() == null
                    || response.getRoute().getPaths().isEmpty()) {
                return "未找到可行路线，请检查起点和终点是否在同一城市或距离是否过远";
            }

            // 格式化返回结果
            return formatRouteResult(response, type.getDisplayName(), origin, destination);

        } catch (Exception e) {
            log.error("{}路线规划异常：{}", type.getDisplayName(), e.getMessage(), e);
            return "路线规划服务暂时不可用，请稍后重试";
        }
    }


    /**
     * 格式化路线规划结果
     *
     * @param response    路线规划响应
     * @param routeType   路线类型（驾车/步行/骑行）
     * @param origin      原始出发地
     * @param destination 原始目的地
     * @return 格式化后的结果字符串
     */
    private String formatRouteResult(RouteResponse response, String routeType, String origin, String destination) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s路线规划结果：\n", routeType));
        result.append(String.format("出发地：%s\n", origin));
        result.append(String.format("目的地：%s\n\n", destination));

        RouteResponse.Route route = response.getRoute();
        if (route.getPaths() != null && !route.getPaths().isEmpty()) {
            RouteResponse.Route.Path firstPath = route.getPaths().get(0);

            // 距离（米转公里）- 兼容null值
            if (firstPath.getDistance() != null && !firstPath.getDistance().isEmpty()) {
                try {
                    double distanceKm = Double.parseDouble(firstPath.getDistance()) / 1000.0;
                    result.append(String.format("路线距离：%.2f公里（%s米）\n", distanceKm, firstPath.getDistance()));
                } catch (NumberFormatException e) {
                    log.warn("距离格式解析失败：{}", firstPath.getDistance());
                    result.append(String.format("路线距离：%s米\n", firstPath.getDistance()));
                }
            }

            // 耗时（秒转分钟）- 兼容null值
            if (firstPath.getDuration() != null && !firstPath.getDuration().isEmpty()) {
                try {
                    int durationSeconds = Integer.parseInt(firstPath.getDuration());
                    int minutes = durationSeconds / 60;
                    int seconds = durationSeconds % 60;
                    result.append(String.format("预计耗时：%d分钟%d秒\n", minutes, seconds));
                } catch (NumberFormatException e) {
                    log.warn("耗时格式解析失败：{}", firstPath.getDuration());
                    result.append(String.format("预计耗时：%s秒\n", firstPath.getDuration()));
                }
            }

            // 收费信息（仅驾车）
            if ("驾车".equals(routeType) && firstPath.getTolls() != null && !firstPath.getTolls().equals("0")) {
                result.append(String.format("过路费：%s元\n", firstPath.getTolls()));
            }

            // 红绿灯数量
            if (firstPath.getTraffic_lights() != null) {
                result.append(String.format("红绿灯数量：%s个\n", firstPath.getTraffic_lights()));
            }

            // 限行状态
            if (firstPath.getRestriction() != null) {
                result.append(String.format("限行状态：%s\n", "1".equals(firstPath.getRestriction()) ? "限行" : "不限行"));
            }

            // 路线步骤（前3步）
            if (firstPath.getSteps() != null && !firstPath.getSteps().isEmpty()) {
                result.append("\n路线指引：\n");
                int stepCount = Math.min(3, firstPath.getSteps().size());
                for (int i = 0; i < stepCount; i++) {
                    RouteResponse.Route.Path.Step step = firstPath.getSteps().get(i);
                    if (step.getInstruction() != null) {
                        String stepInfo = String.format("%d. %s", i + 1, step.getInstruction());
                        // 如果步骤有距离信息，则添加距离
                        if (step.getDistance() != null && !step.getDistance().isEmpty()) {
                            try {
                                double stepDistanceKm = Double.parseDouble(step.getDistance()) / 1000.0;
                                stepInfo += String.format("（%.2f公里）", stepDistanceKm);
                            } catch (NumberFormatException e) {
                                // 如果解析失败，直接显示米数
                                stepInfo += String.format("（%s米）", step.getDistance());
                            }
                        }
                        result.append(stepInfo).append("\n");
                    }
                }
                if (firstPath.getSteps().size() > 3) {
                    result.append(String.format("... 共%d个路段\n", firstPath.getSteps().size()));
                }
            }
        }

        return result.toString();
    }
}


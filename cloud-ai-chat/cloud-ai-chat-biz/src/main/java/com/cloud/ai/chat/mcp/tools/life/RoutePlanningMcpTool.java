package com.cloud.ai.chat.mcp.tools.life;


import com.cloud.ai.chat.domain.RouteResponse;
import com.cloud.ai.chat.enums.RouteType;
import com.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.ai.chat.mcp.api.Schema;
import com.cloud.ai.chat.mcp.service.LocationService;
import com.cloud.ai.chat.mcp.service.RoutePlanningService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 路线规划工具 - 基于MCP架构的实现
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoutePlanningMcpTool implements McpTool {

    private final RoutePlanningService routePlanningService;
    private final LocationService locationService;

    @Override
    public String getName() {
        return "plan_route";
    }

    @Override
    public String getDescription() {
        return "规划路线。支持三种路线类型：drive（驾车）、walk（步行）、ride（骑行）。" +
                "出发地和目的地都可以是地址（如\"北京市天安门\"）或坐标（格式：经度,纬度，如\"116.434307,39.90909\"）。" +
                "系统会自动将地址转换为坐标后再规划路线。返回路线距离、耗时等信息。驾车路线还会返回费用信息。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();

        properties.put("from", Schema.string("出发地，可以是地址（如\"北京市天安门\"）或坐标（格式：经度,纬度，如\"116.434307,39.90909\"）"));
        properties.put("to", Schema.string("目的地，可以是地址（如\"北京市故宫\"）或坐标（格式：经度,纬度，如\"116.434446,39.90816\"）"));
        properties.put("mode", Schema.stringEnum(
                "路线类型：drive（驾车）、walk（步行）、ride（骑行）",
                Arrays.asList("drive", "walk", "ride")
        ));

        // from和to是必填的，mode可选（默认drive）
        return Schema.object(properties, Arrays.asList("from", "to"));
    }

    @Override
    public Schema getOutputSchema() {
        return Schema.string("格式化后的路线规划结果，包含距离、耗时、费用、路线指引等信息");
    }

    @Override
    public boolean match(String query) {
        if (query == null) {
            return false;
        }
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("路线") ||
                lowerQuery.contains("导航") ||
                lowerQuery.contains("怎么去") ||
                lowerQuery.contains("如何到") ||
                lowerQuery.contains("route") ||
                lowerQuery.contains("怎么走");
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        // 解析参数
        String from = input.get("from").asText();
        String to = input.get("to").asText();
        String mode = input.has("mode") ? input.get("mode").asText() : "drive";

        // 将mode转换为RouteType
        RouteType routeType = switch (mode.toLowerCase()) {
            case "walk" -> RouteType.WALKING;
            case "ride" -> RouteType.BICYCLING;
            default -> RouteType.DRIVING;
        };

        log.info("开始规划{}路线，出发地：{}，目的地：{}", routeType.getDisplayName(), from, to);

        try {
            // 获取出发地坐标（支持地址或坐标格式）
            String originCoord = locationService.getCoordinate(from);
            if (originCoord == null) {
                return "无法获取出发地坐标，请检查地址是否正确（例如：北京市天安门）或坐标格式是否正确（格式：经度,纬度）";
            }

            // 获取目的地坐标（支持地址或坐标格式）
            String destinationCoord = locationService.getCoordinate(to);
            if (destinationCoord == null) {
                return "无法获取目的地坐标，请检查地址是否正确（例如：北京市故宫）或坐标格式是否正确（格式：经度,纬度）";
            }

            // 调用路线规划服务
            RouteResponse response = routePlanningService.planRoute(routeType, originCoord, destinationCoord)
                    .timeout(Duration.ofSeconds(15))
                    .doOnSubscribe(s -> log.debug("订阅{}路线规划服务", routeType.getDisplayName()))
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
            return formatRouteResult(response, routeType.getDisplayName(), from, to);

        } catch (Exception e) {
            log.error("{}路线规划异常：{}", routeType.getDisplayName(), e.getMessage(), e);
            return "路线规划服务暂时不可用，请稍后重试";
        }
    }

    @Override
    public String getCategory() {
        return "life";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    /**
     * 格式化路线规划结果
     */
    private String formatRouteResult(RouteResponse response, String routeType, String origin, String destination) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("%s路线规划结果：\n", routeType));
        result.append(String.format("出发地：%s\n", origin));
        result.append(String.format("目的地：%s\n\n", destination));

        RouteResponse.Route route = response.getRoute();
        if (route.getPaths() != null && !route.getPaths().isEmpty()) {
            RouteResponse.Route.Path firstPath = route.getPaths().get(0);

            // 距离（米转公里）
            if (firstPath.getDistance() != null && !firstPath.getDistance().isEmpty()) {
                try {
                    double distanceKm = Double.parseDouble(firstPath.getDistance()) / 1000.0;
                    result.append(String.format("路线距离：%.2f公里（%s米）\n", distanceKm, firstPath.getDistance()));
                } catch (NumberFormatException e) {
                    log.warn("距离格式解析失败：{}", firstPath.getDistance());
                    result.append(String.format("路线距离：%s米\n", firstPath.getDistance()));
                }
            }

            // 耗时（秒转分钟）
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
                        if (step.getDistance() != null && !step.getDistance().isEmpty()) {
                            try {
                                double stepDistanceKm = Double.parseDouble(step.getDistance()) / 1000.0;
                                stepInfo += String.format("（%.2f公里）", stepDistanceKm);
                            } catch (NumberFormatException e) {
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

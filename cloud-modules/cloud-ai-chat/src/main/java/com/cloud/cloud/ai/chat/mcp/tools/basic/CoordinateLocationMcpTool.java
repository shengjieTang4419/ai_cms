package com.cloud.cloud.ai.chat.mcp.tools.basic;

import com.cloud.cloud.ai.chat.domain.RegeoResponse;
import com.cloud.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.cloud.ai.chat.mcp.api.Schema;
import com.cloud.cloud.ai.chat.mcp.service.LocationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 坐标定位工具 - 基于MCP架构的实现
 * 通过经纬度坐标获取详细地址信息
 *
 * @author shengjie.tang
 * @version 2.0.0
 * @date 2025/11/16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoordinateLocationMcpTool implements McpTool {

    private final LocationService locationService;

    @Override
    public String getName() {
        return "get_location_by_coordinates";
    }

    @Override
    public String getDescription() {
        return "根据经纬度坐标获取详细地址信息，返回完整地址、省份、城市、区县、街道等信息。精度为街道级别，精度较高。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("longitude", Schema.string("经度，例如：121.473701（上海）"));
        properties.put("latitude", Schema.string("纬度，例如：31.230416（上海）"));
        return Schema.object(properties, Arrays.asList("longitude", "latitude"));
    }

    @Override
    public Schema getOutputSchema() {
        return Schema.string("详细地址信息，包含完整地址、省份、城市、区县、街道等");
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        String longitude = input.get("longitude").asText();
        String latitude = input.get("latitude").asText();

        log.info("开始获取坐标定位信息，经度：{}，纬度：{}", longitude, latitude);

        try {
            // 验证坐标格式
            try {
                Double.parseDouble(longitude);
                Double.parseDouble(latitude);
            } catch (NumberFormatException e) {
                log.warn("坐标格式错误：经度={}，纬度={}", longitude, latitude);
                return String.format("坐标格式错误。经度应为数字（例如：121.473701），纬度应为数字（例如：31.230416）。您提供的坐标：经度=%s，纬度=%s", longitude, latitude);
            }

            return locationService.getLocationByCoordinates(longitude, latitude)
                    .timeout(Duration.ofSeconds(10))
                    .map(response -> {
                        log.info("逆地理编码API调用成功：{}", response);
                        if (!"1".equals(response.getStatus())) {
                            log.error("逆地理编码API返回错误状态：{}", response.getInfo());
                            return String.format("定位服务暂时不可用：%s。请稍后重试。", response.getInfo());
                        }

                        if (response.getRegeocode() == null) {
                            log.warn("逆地理编码API返回数据异常：无地址信息");
                            return "未获取到地址信息";
                        }

                        RegeoResponse.Regeocode regeocode = response.getRegeocode();
                        StringBuilder result = new StringBuilder();

                        if (regeocode.getFormatted_address() != null) {
                            result.append("地址：").append(regeocode.getFormatted_address());
                        }

                        if (regeocode.getAddressComponent() != null) {
                            RegeoResponse.AddressComponent addr = regeocode.getAddressComponent();
                            result.append("\n详细信息：");
                            if (addr.getCountry() != null) {
                                result.append("\n  国家：").append(addr.getCountry());
                            }
                            if (addr.getProvince() != null) {
                                result.append("\n  省份：").append(addr.getProvince());
                            }
                            if (addr.getCity() != null) {
                                result.append("\n  城市：").append(addr.getCity());
                            }
                            if (addr.getDistrict() != null) {
                                result.append("\n  区县：").append(addr.getDistrict());
                            }
                            if (addr.getTownship() != null) {
                                result.append("\n  街道：").append(addr.getTownship());
                            }
                            if (addr.getStreet() != null) {
                                result.append("\n  道路：").append(addr.getStreet());
                            }
                            if (addr.getStreetNumber() != null) {
                                result.append("\n  门牌号：").append(addr.getStreetNumber());
                            }
                            if (addr.getAdcode() != null) {
                                result.append("\n  区域编码：").append(addr.getAdcode());
                            }
                        }

                        result.append("\n坐标：").append(longitude).append(",").append(latitude);

                        return result.toString();
                    })
                    .doOnError(error -> log.error("坐标定位查询失败：{}", error.getMessage(), error))
                    .onErrorReturn("抱歉，坐标定位服务暂时出现故障，请稍后重试。")
                    .block();
        } catch (Exception e) {
            log.error("坐标定位查询异常：{}", e.getMessage(), e);
            return "坐标定位服务暂时不可用，请稍后重试。";
        }
    }

    @Override
    public String getCategory() {
        return "basic";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }
}

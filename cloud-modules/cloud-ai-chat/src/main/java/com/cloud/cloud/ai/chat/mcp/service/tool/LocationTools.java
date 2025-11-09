package com.cloud.cloud.ai.chat.mcp.service.tool;

import com.cloud.cloud.ai.chat.domain.RegeoResponse;
import com.cloud.cloud.ai.chat.mcp.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 定位工具 - 提供IP定位和坐标定位功能
 * @date 2025/10/8 16:00
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationTools {

    private final LocationService locationService;

    /**
     * 通过IP地址获取位置信息
     * 精度：城市级别（精度一般）
     *
     * @param ip IP地址，可选。如果不提供，将使用请求来源的IP地址
     * @return 位置信息描述
     */
    @Tool(name = "get_location_by_ip", description = "根据IP地址获取位置信息，返回省份、城市等信息。精度为城市级别。如果不提供IP参数，系统会自动使用请求来源的IP地址进行定位。这是获取用户当前位置的主要方法。")
    public String getLocationByIp(@ToolParam(description = "IP地址，例如：114.114.114.114。如果不提供或为空，将自动使用请求来源的IP地址", required = false) String ip) {
        log.info("开始获取IP定位信息，IP地址：{}", ip != null ? ip : "使用请求IP");

        try {
            return locationService.getLocationByIp(ip)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSubscribe(s -> log.debug("订阅IP定位服务"))
                    .map(response -> {
                        log.info("IP定位API调用成功：{}", response);

                        // 检查API响应状态
                        if (!"1".equals(response.getStatus())) {
                            log.error("IP定位API返回错误状态：{}", response.getInfo());
                            return String.format("定位服务暂时不可用：%s。请稍后重试。", response.getInfo());
                        }

                        // 构建返回信息
                        StringBuilder result = new StringBuilder();
                        if (response.getProvince() != null) {
                            result.append("省份：").append(response.getProvince());
                        }
                        if (response.getCity() != null) {
                            if (!result.isEmpty()) result.append("，");
                            result.append("城市：").append(response.getCity());
                        }
                        if (response.getAdcode() != null) {
                            if (!result.isEmpty()) result.append("，");
                            result.append("区域编码：").append(response.getAdcode());
                        }
                        if (ip != null && !ip.isEmpty()) {
                            result.append("（IP：").append(ip).append("）");
                        }

                        return !result.isEmpty() ? result.toString() : "未获取到位置信息";
                    })
                    .doOnError(error -> {
                        log.error("IP定位查询失败：{}", error.getMessage(), error);
                    })
                    .onErrorReturn("抱歉，IP定位服务暂时出现故障，请稍后重试。")
                    .block();  // 阻塞等待结果
        } catch (Exception e) {
            log.error("IP定位查询异常：{}", e.getMessage(), e);
            return "IP定位服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 通过经纬度坐标获取详细地址信息
     * 精度：街道级别（精度高）
     *
     * @param longitude 经度，例如：121.473701
     * @param latitude  纬度，例如：31.230416
     * @return 详细地址信息
     */
    @Tool(name = "get_location_by_coordinates", description = "根据经纬度坐标获取详细地址信息，返回完整地址、省份、城市、区县、街道等信息。精度为街道级别，精度较高。")
    public String getLocationByCoordinates(
            @ToolParam(description = "经度，例如：121.473701（上海）") String longitude,
            @ToolParam(description = "纬度，例如：31.230416（上海）") String latitude) {
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
                    .timeout(java.time.Duration.ofSeconds(10))  // 设置10秒超时
                    .doOnSubscribe(s -> log.debug("订阅逆地理编码服务"))
                    .map(response -> {
                        log.info("逆地理编码API调用成功：{}", response);

                        // 检查API响应状态
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

                        // 格式化地址
                        if (regeocode.getFormatted_address() != null) {
                            result.append("地址：").append(regeocode.getFormatted_address());
                        }

                        // 详细地址组件
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
                    .doOnError(error -> {
                        log.error("坐标定位查询失败：{}", error.getMessage(), error);
                    })
                    .onErrorReturn("抱歉，坐标定位服务暂时出现故障，请稍后重试。")
                    .block();  // 阻塞等待结果
        } catch (Exception e) {
            log.error("坐标定位查询异常：{}", e.getMessage(), e);
            return "坐标定位服务暂时不可用，请稍后重试。";
        }
    }
}


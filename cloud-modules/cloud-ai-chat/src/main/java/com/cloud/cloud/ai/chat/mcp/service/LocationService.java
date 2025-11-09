package com.cloud.cloud.ai.chat.mcp.service;

import com.cloud.cloud.ai.chat.domain.GeocodeResponse;
import com.cloud.cloud.ai.chat.domain.IpLocationResponse;
import com.cloud.cloud.ai.chat.domain.RegeoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 定位服务 - 提供IP定位和逆地理编码功能
 * @date 2025/10/8 16:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    @Value("${map.api-key}")
    private String apiKey;

    //Spring name match 注入问题 匹配MapConfig的amapWebClient
    private final WebClient amapWebClient;

    /**
     * IP定位 - 根据IP地址获取位置信息
     * 精度：城市级别（精度一般）
     *
     * @param ip IP地址，如果为空则使用请求IP
     * @return 定位信息
     */
    public Mono<IpLocationResponse> getLocationByIp(String ip) {
        return amapWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v3/ip")
                            .queryParam("key", apiKey)
                            .queryParam("output", "JSON");
                    if (ip != null && !ip.isEmpty()) {
                        uriBuilder.queryParam("ip", ip);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(IpLocationResponse.class)
                .doOnSuccess(response -> log.info("IP定位成功: {}", response))
                .doOnError(error -> log.error("IP定位失败", error));
    }

    /**
     * 逆地理编码 - 根据经纬度获取地址信息
     * 精度：精确到街道级别（精度高）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 地址信息
     */
    public Mono<RegeoResponse> getLocationByCoordinates(String longitude, String latitude) {
        return amapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/geocode/regeo")
                        .queryParam("key", apiKey)
                        .queryParam("location", longitude + "," + latitude)
                        .queryParam("output", "JSON")
                        .queryParam("radius", "1000")  // 搜索半径，单位：米
                        .queryParam("extensions", "all")  // 返回详细信息
                        .build())
                .retrieve()
                .bodyToMono(RegeoResponse.class)
                .doOnSuccess(response -> log.info("逆地理编码成功: {}", response))
                .doOnError(error -> log.error("逆地理编码失败", error));
    }

    /**
     * 通过IP定位获取城市编码（同步方法）
     * 封装了IP定位的完整流程：调用API、验证响应、错误处理
     *
     * @param ip IP地址，如果为null或空，则使用请求来源的IP地址
     * @return 城市编码（adcode），如果获取失败返回null
     */
    public String getCityCodeByIp(String ip) {
        try {
            IpLocationResponse response = getLocationByIp(ip)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(r -> log.info("IP定位API调用成功：{}", r))
                    .doOnError(error -> log.error("IP定位查询失败：{}", error.getMessage(), error))
                    .block();  // 阻塞等待结果

            if (response == null) {
                log.warn("IP定位返回响应为空");
                return null;
            }

            // 检查API响应状态
            if (!"1".equals(response.getStatus())) {
                log.error("IP定位API返回错误状态：{}", response.getInfo());
                return null;
            }

            String cityCode = response.getAdcode();
            if (cityCode == null || cityCode.isEmpty()) {
                log.warn("IP定位未获取到城市编码，省份={}，城市={}", response.getProvince(), response.getCity());
                return null;
            }

            log.info("IP定位成功：省份={}，城市={}，城市编码={}", response.getProvince(), response.getCity(), cityCode);
            return cityCode;
        } catch (Exception e) {
            log.error("IP定位查询异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据经纬度获取详细地址信息（同步方法，带完整校验和错误处理）
     * 封装了逆地理编码的完整流程：验证坐标、调用API、验证响应、错误处理
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 详细地址信息，如果失败返回null
     */
    public RegeoResponse getDetailedAddressByCoordinates(String longitude, String latitude) {
        // 验证坐标格式
        try {
            Double.parseDouble(longitude);
            Double.parseDouble(latitude);
        } catch (NumberFormatException e) {
            log.warn("坐标格式错误：经度={}，纬度={}", longitude, latitude);
            return null;
        }

        try {
            RegeoResponse response = getLocationByCoordinates(longitude, latitude)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(r -> log.info("逆地理编码API调用成功：{}", r))
                    .doOnError(error -> log.error("逆地理编码查询失败：{}", error.getMessage(), error))
                    .block();  // 阻塞等待结果

            if (response == null) {
                log.warn("逆地理编码返回响应为空");
                return null;
            }

            // 检查API响应状态
            if (!"1".equals(response.getStatus())) {
                log.error("逆地理编码API返回错误状态：{}", response.getInfo());
                return null;
            }

            if (response.getRegeocode() == null) {
                log.warn("逆地理编码API返回数据异常：无地址信息");
                return null;
            }

            log.info("逆地理编码成功：经度={}，纬度={}，地址={}",
                    longitude, latitude, response.getRegeocode().getFormatted_address());
            return response;
        } catch (Exception e) {
            log.error("逆地理编码查询异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 地理编码 - 根据地址获取坐标
     * 精度：根据地址精确度而定
     *
     * @param address 地址，例如：北京市天安门
     * @return 地理编码响应
     */
    public Mono<GeocodeResponse> getCoordinatesByAddress(String address) {
        return amapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/geocode/geo")
                        .queryParam("key", apiKey)
                        .queryParam("address", address)
                        .queryParam("output", "JSON")
                        .build())
                .retrieve()
                .bodyToMono(GeocodeResponse.class)
                .doOnSuccess(response -> log.info("地理编码成功: address={}", address))
                .doOnError(error -> log.error("地理编码失败: address={}", address, error));
    }

    /**
     * 获取坐标（统一方法）
     * 如果输入是坐标格式（经度,纬度），直接返回；如果是地址，调用地理编码获取坐标
     *
     * @param input 输入（地址或坐标，格式：经度,纬度）
     * @return 坐标字符串（格式：经度,纬度），失败返回null
     */
    public String getCoordinate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        input = input.trim();

        // 检查是否是坐标格式（经度,纬度）
        if (isCoordinateFormat(input)) {
            log.debug("输入是坐标格式，直接返回：{}", input);
            return input;
        }

        // 否则认为是地址，需要进行地理编码
        log.info("输入是地址文本，进行地理编码：{}", input);
        return getCoordinateByAddress(input);
    }

    /**
     * 根据地址获取坐标（同步方法，带完整校验和错误处理）
     *
     * @param address 地址
     * @return 坐标字符串（格式：经度,纬度），失败返回null
     */
    public String getCoordinateByAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        try {
            GeocodeResponse response = getCoordinatesByAddress(address)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(r -> log.info("地理编码API调用成功：{}", r))
                    .doOnError(error -> log.error("地理编码查询失败：{}", error.getMessage(), error))
                    .block();

            if (response == null) {
                log.warn("地理编码返回响应为空");
                return null;
            }

            // 检查API响应状态
            if (!"1".equals(response.getStatus())) {
                log.error("地理编码API返回错误状态：{}", response.getInfo());
                return null;
            }

            if (response.getGeocodes() == null || response.getGeocodes().isEmpty()) {
                log.warn("地理编码API返回数据异常：无坐标信息");
                return null;
            }

            GeocodeResponse.Geocode geocode = response.getGeocodes().get(0);
            String location = geocode.getLocation();

            log.info("地理编码成功：地址={}，坐标={}", address, location);
            return location;
        } catch (Exception e) {
            log.error("地理编码查询异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查是否是坐标格式（经度,纬度）
     * 经度在前，纬度在后，经度和纬度用","分割，经纬度小数点后不得超过6位
     *
     * @param input 输入字符串
     * @return 是否是坐标格式
     */
    private boolean isCoordinateFormat(String input) {
        // 简单检查：包含逗号，且逗号前后都是数字
        if (!input.contains(",")) {
            return false;
        }

        String[] parts = input.split(",");
        if (parts.length != 2) {
            return false;
        }

        try {
            // 验证经度范围：-180 到 180
            double longitude = Double.parseDouble(parts[0].trim());
            if (longitude < -180 || longitude > 180) {
                return false;
            }

            // 验证纬度范围：-90 到 90
            double latitude = Double.parseDouble(parts[1].trim());
            return !(latitude < -90) && !(latitude > 90);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}


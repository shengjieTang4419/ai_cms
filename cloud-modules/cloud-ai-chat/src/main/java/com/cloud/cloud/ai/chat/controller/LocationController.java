package com.cloud.cloud.ai.chat.controller;

import com.cloud.cloud.ai.chat.domain.RegeoResponse;
import com.cloud.cloud.ai.chat.mcp.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 定位控制器 - 提供前端调用获取详细地址的API
 * @date 2025/01/17
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    /**
     * 根据经纬度获取详细地址信息
     * 精度：街道级别（精度高）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 详细地址信息
     */
    @GetMapping("/coordinates")
    public ResponseEntity<Map<String, Object>> getLocationByCoordinates(
            @RequestParam("longitude") String longitude,
            @RequestParam("latitude") String latitude) {
        
        log.info("接收前端定位请求，经度：{}，纬度：{}", longitude, latitude);

        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务获取地址信息（服务层已包含所有校验逻辑）
            RegeoResponse regeoResponse = locationService.getDetailedAddressByCoordinates(longitude, latitude);

            if (regeoResponse == null) {
                response.put("success", false);
                response.put("message", "定位服务暂时不可用，请稍后重试");
                return ResponseEntity.ok().body(response);
            }

            // 构建返回数据
            RegeoResponse.Regeocode regeocode = regeoResponse.getRegeocode();
            Map<String, Object> data = new HashMap<>();

            // 格式化地址
            if (regeocode.getFormatted_address() != null) {
                data.put("formattedAddress", regeocode.getFormatted_address());
            }

            // 详细地址组件
            if (regeocode.getAddressComponent() != null) {
                RegeoResponse.AddressComponent addr = regeocode.getAddressComponent();
                Map<String, String> addressComponent = new HashMap<>();
                
                if (addr.getCountry() != null) {
                    addressComponent.put("country", addr.getCountry());
                }
                if (addr.getProvince() != null) {
                    addressComponent.put("province", addr.getProvince());
                }
                if (addr.getCity() != null) {
                    addressComponent.put("city", addr.getCity());
                }
                if (addr.getDistrict() != null) {
                    addressComponent.put("district", addr.getDistrict());
                }
                if (addr.getTownship() != null) {
                    addressComponent.put("township", addr.getTownship());
                }
                if (addr.getStreet() != null) {
                    addressComponent.put("street", addr.getStreet());
                }
                if (addr.getStreetNumber() != null) {
                    addressComponent.put("streetNumber", addr.getStreetNumber());
                }
                if (addr.getAdcode() != null) {
                    addressComponent.put("adcode", addr.getAdcode());
                }
                
                data.put("addressComponent", addressComponent);
            }

            data.put("longitude", longitude);
            data.put("latitude", latitude);

            response.put("success", true);
            response.put("data", data);
            
            log.info("成功返回详细地址信息：{}", data);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("坐标定位查询异常：{}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "定位服务暂时不可用，请稍后重试");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}


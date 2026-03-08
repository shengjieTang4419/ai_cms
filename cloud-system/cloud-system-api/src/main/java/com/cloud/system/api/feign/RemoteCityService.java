package com.cloud.system.api.feign;


import com.cloud.common.core.constant.ServiceNameConstants;
import com.cloud.common.core.domain.R;
import com.cloud.system.api.dto.CityDTO;
import com.cloud.system.api.factory.RemoteCityServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 远程调用城市信息服务
 * @date 2026/1/2 12:11
 */
@FeignClient(contextId = "remoteCityService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteCityServiceFallbackFactory.class)
public interface RemoteCityService {

    @GetMapping("/getByCode/{cityCode}")
    public R<CityDTO> getCityByCode(@PathVariable("cityCode") String cityCode);

    @GetMapping("/getByName/{cityName}")
    public R<CityDTO> getCityByName(@PathVariable("cityName") String cityName);
}

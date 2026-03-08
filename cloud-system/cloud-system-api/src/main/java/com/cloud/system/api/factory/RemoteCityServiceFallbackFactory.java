package com.cloud.system.api.factory;


import com.cloud.common.core.domain.R;
import com.cloud.system.api.dto.CityDTO;
import com.cloud.system.api.feign.RemoteCityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2026/1/2 12:12
 */
@Component
@Slf4j
public class RemoteCityServiceFallbackFactory implements FallbackFactory<RemoteCityService> {
    @Override
    public RemoteCityService create(Throwable cause) {
        log.error("RemoteCityService fallback: ", cause.getMessage());
        return new RemoteCityService() {

            @Override
            public R<CityDTO> getCityByCode(String cityCode) {
                return R.fail("通过code" + cityCode + "获取城市失败:" + cause.getMessage());
            }

            @Override
            public R<CityDTO> getCityByName(String cityName) {
                return R.fail("通过name" + cityName + "获取城市失败:" + cause.getMessage());
            }
        };
    }
}

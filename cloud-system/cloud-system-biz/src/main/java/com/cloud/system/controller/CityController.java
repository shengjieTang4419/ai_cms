package com.cloud.system.controller;


import com.cloud.common.core.converter.BeanConverter;
import com.cloud.common.core.domain.R;
import com.cloud.common.core.web.controller.BaseController;
import com.cloud.system.api.dto.CityDTO;
import com.cloud.system.domain.CityInfo;
import com.cloud.system.service.CityInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2026/1/2 11:38
 */
@Slf4j
@RestController
@RequestMapping("/city")
@RequiredArgsConstructor
public class CityController extends BaseController {

    private final CityInfoService cityInfoService;

    @GetMapping("/list")
    public R<List<CityInfo>> list() {
        return R.ok(cityInfoService.getAllCities());
    }

    @GetMapping("/getByCode/{cityCode}")
    public R<CityDTO> getCityByCode(@PathVariable("cityCode") String cityCode) {
        CityInfo cityInfo = cityInfoService.getCityInfoByCode(cityCode);
        return R.ok(BeanConverter.convert(cityInfo, CityDTO.class));
    }

    @GetMapping("/getByName/{cityName}")
    public R<CityDTO> getCityByName(@PathVariable("cityName") String cityName) {
        CityInfo cityInfo = cityInfoService.getCityInfoByName(cityName);
        return R.ok(BeanConverter.convert(cityInfo, CityDTO.class));
    }
}

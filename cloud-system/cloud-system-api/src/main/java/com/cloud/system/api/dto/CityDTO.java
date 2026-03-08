package com.cloud.system.api.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: CityDTO
 * @date 2026/1/2 11:51
 */
@Data
public class CityDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String province;
    private String city;
    private String district;
    private String amapCityCode;
    private String adminCode;
    private Integer level = 1;
    private String parentCode;
    private String fullName;
    private Double longitude;
    private Double latitude;
    private Integer enabled = 1;
    private String remark;
}

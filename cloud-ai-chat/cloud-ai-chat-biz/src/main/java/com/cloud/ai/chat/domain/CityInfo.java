package com.cloud.ai.chat.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 城市信息实体类
 * 用于存储城市编码和行政区划信息
 *
 * @author shengjie.tang
 * @version 1.0.0
 */
@Data
@Entity
@Table(name = "city_info")
public class CityInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 城市名称（中文）
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 省份名称
     */
    @Column(name = "province", length = 50)
    private String province;

    /**
     * 城市名称（不含省份）
     */
    @Column(name = "city", length = 50)
    private String city;

    /**
     * 区县名称（如：浦东新区、朝阳区等）
     */
    @Column(name = "district", length = 50)
    private String district;

    /**
     * 高德地图城市编码（6位）
     * 例如：北京=110000，上海=310000
     */
    @Column(name = "amap_city_code", length = 10)
    private String amapCityCode;

    /**
     * 国家统计局行政区划代码（6位或12位）
     * 例如：北京市=110000，上海市=310000
     */
    @Column(name = "admin_code", length = 20)
    private String adminCode;

    /**
     * 行政级别
     * 1-省，2-市，3-区县，4-街道
     */
    @Column(name = "level")
    private Integer level = 1;

    /**
     * 父级行政区划代码
     */
    @Column(name = "parent_code", length = 20)
    private String parentCode;

    /**
     * 完整路径名称（省市区组合）
     * 例如：上海市浦东新区
     */
    @Column(name = "full_name", length = 200)
    private String fullName;

    /**
     * 经度
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * 纬度
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    @Column(name = "enabled")
    private Integer enabled = 1;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

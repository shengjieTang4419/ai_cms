package com.cloud.common.security.dto;

import lombok.Data;

/**
 * 设备信息
 */
@Data
public class DeviceInfo {
    
    /**
     * 设备唯一标识
     */
    private String deviceId;
    
    /**
     * 设备类型 (WEB, MOBILE, TABLET, DESKTOP)
     */
    private String deviceType;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 操作系统
     */
    private String os;
    
    /**
     * 浏览器信息
     */
    private String browser;
    
    /**
     * 设备型号 (移动端)
     */
    private String model;
    
    /**
     * 是否为移动设备
     */
    private Boolean isMobile;
    
}

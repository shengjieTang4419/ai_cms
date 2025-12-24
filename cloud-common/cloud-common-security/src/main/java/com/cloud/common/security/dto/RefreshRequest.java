package com.cloud.common.security.dto;

import lombok.Data;

/**
 * 刷新Token请求
 */
@Data
public class RefreshRequest {

    /**
     * 刷新Token
     */
    private String refreshToken;
    
    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;

}

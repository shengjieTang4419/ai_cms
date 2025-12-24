package com.cloud.common.security.dto;

import com.cloud.common.core.constant.DeviceType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Token刷新请求参数对象
 *
 * @author shengjie.tang
 */
@Data
@Builder
public class TokenRefreshRequest {
    
    private String userKey;
    
    private Integer userId;
    
    private String username;
    
    private String deviceId;
    
    private DeviceType deviceType;
    
    private Map<String, Object> oldTokenInfo;
    
    private String currentIp;
}

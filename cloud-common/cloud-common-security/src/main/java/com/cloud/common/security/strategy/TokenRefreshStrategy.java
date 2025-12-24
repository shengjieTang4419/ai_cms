package com.cloud.common.security.strategy;

import com.cloud.common.security.dto.TokenRefreshRequest;

import java.util.Map;

/**
 * Token刷新策略接口
 *
 * @author shengjie.tang
 */
public interface TokenRefreshStrategy {
    
    /**
     * 刷新Token
     *
     * @param request 刷新请求参数
     * @return Token信息
     */
    Map<String, Object> refreshToken(TokenRefreshRequest request);
    
    /**
     * 是否支持该设备类型
     */
    boolean support(String deviceType);
}

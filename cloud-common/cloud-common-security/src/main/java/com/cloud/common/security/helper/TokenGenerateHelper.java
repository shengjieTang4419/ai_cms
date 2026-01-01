package com.cloud.common.security.helper;

import com.cloud.common.core.constant.DeviceType;
import com.cloud.common.core.constant.SecurityConstants;
import com.cloud.common.core.constant.TokenStrategy;
import com.cloud.common.core.util.JwtUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Token生成帮助类
 *
 * @author shengjie.tang
 */
@Component
public class TokenGenerateHelper {

    /**
     * 生成AccessToken
     */
    public String generateAccessToken(String userKey, Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.USER_KEY, userKey);
        claims.put(SecurityConstants.DETAILS_USER_ID, userId);
        claims.put(SecurityConstants.DETAILS_USERNAME, username);
        claims.put(SecurityConstants.TOKEN_TYPE, SecurityConstants.ACCESS_TOKEN_TYPE);
        return JwtUtils.createToken(claims);
    }

    /**
     * 生成RefreshToken
     */
    public String generateRefreshToken(String userKey, Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.USER_KEY, userKey);
        claims.put(SecurityConstants.DETAILS_USER_ID, userId);
        claims.put(SecurityConstants.DETAILS_USERNAME, username);
        claims.put(SecurityConstants.TOKEN_TYPE, SecurityConstants.REFRESH_TOKEN_TYPE);
        return JwtUtils.createRefreshToken(claims);
    }

    /**
     * 构建Token响应
     */
    public Map<String, Object> buildTokenResponse(String accessToken, String refreshToken,
                                                  DeviceType deviceType, long accessTokenExpire,
                                                  long refreshTokenExpire) {
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("refresh_token", refreshToken);
        result.put("expires_in", accessTokenExpire);
        result.put("refresh_expires_in", refreshTokenExpire);
        result.put("device_type", deviceType.getCode());
        result.put("strategy", TokenStrategy.getStrategyDescription(deviceType));
        return result;
    }
}

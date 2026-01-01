package com.cloud.common.security.strategy;

import com.cloud.common.core.constant.DeviceType;
import com.cloud.common.core.constant.TokenStrategy;
import com.cloud.common.security.dto.TokenRefreshRequest;
import com.cloud.common.security.helper.TokenCacheHelper;
import com.cloud.common.security.helper.TokenGenerateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 移动端Token刷新策略（重复消费）
 *
 * @author shengjie.tang
 */
@Component
@RequiredArgsConstructor
public class MobileTokenRefreshStrategy implements TokenRefreshStrategy {

    private final TokenGenerateHelper tokenGenerateHelper;
    private final TokenCacheHelper tokenCacheHelper;

    @Override
    public Map<String, Object> refreshToken(TokenRefreshRequest request) {
        DeviceType deviceType = request.getDeviceType();
        long accessTokenExpire = TokenStrategy.getAccessTokenExpire(deviceType);
        long refreshTokenExpire = TokenStrategy.getRefreshTokenExpire(deviceType);

        // 复用旧的userKey（重复消费的核心）
        String userKey = request.getUserKey();

        // 使用Helper生成新的AccessToken
        String newAccessToken = tokenGenerateHelper.generateAccessToken(
                userKey, request.getUserId(), request.getUsername());

        // RefreshToken不变，复用旧的
        String oldRefreshToken = (String) request.getOldTokenInfo().get("refreshToken");

        // 使用Helper缓存AccessToken
        tokenCacheHelper.cacheAccessToken(userKey, newAccessToken, request.getUserId(),
                request.getUsername(), request.getDeviceId(), deviceType,
                request.getCurrentIp(), accessTokenExpire);

        // 使用Helper更新RefreshToken信息（保持原TTL，只更新IP）
        tokenCacheHelper.updateRefreshToken(userKey, request.getCurrentIp());

        // 使用Helper构建响应（RefreshToken不变）
        return tokenGenerateHelper.buildTokenResponse(newAccessToken, oldRefreshToken,
                deviceType, accessTokenExpire, refreshTokenExpire);
    }

    @Override
    public boolean support(String deviceType) {
        return DeviceType.MOBILE.name().equals(deviceType);
    }
}

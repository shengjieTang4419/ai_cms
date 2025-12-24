package com.cloud.common.security.strategy;

import com.cloud.common.core.constant.DeviceType;
import com.cloud.common.core.constant.TokenStrategy;
import com.cloud.common.core.uuid.IdUtils;
import com.cloud.common.security.dto.TokenRefreshRequest;
import com.cloud.common.security.helper.TokenCacheHelper;
import com.cloud.common.security.helper.TokenGenerateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PC端Token刷新策略（一次性消费）
 *
 * @author shengjie.tang
 */
@Component
@RequiredArgsConstructor
public class PcTokenRefreshStrategy implements TokenRefreshStrategy {

    private final TokenGenerateHelper tokenGenerateHelper;
    private final TokenCacheHelper tokenCacheHelper;

    @Override
    public Map<String, Object> refreshToken(TokenRefreshRequest request) {
        DeviceType deviceType = request.getDeviceType();
        long accessTokenExpire = TokenStrategy.getAccessTokenExpire(deviceType);
        long refreshTokenExpire = TokenStrategy.getRefreshTokenExpire(deviceType);

        // 生成新的userKey（一次性消费的核心）
        String newUserKey = IdUtils.fastUUID();

        // 使用Helper生成Token
        String newAccessToken = tokenGenerateHelper.generateAccessToken(
            newUserKey, request.getUserId(), request.getUsername());
        String newRefreshToken = tokenGenerateHelper.generateRefreshToken(
            newUserKey, request.getUserId(), request.getUsername());

        // 使用Helper缓存Token
        tokenCacheHelper.cacheAccessToken(newUserKey, newAccessToken, request.getUserId(),
            request.getUsername(), request.getDeviceId(), deviceType, 
            request.getCurrentIp(), accessTokenExpire);
        tokenCacheHelper.cacheRefreshToken(newUserKey, newRefreshToken, request.getUserId(),
            request.getUsername(), request.getDeviceId(), deviceType, 
            request.getCurrentIp(), refreshTokenExpire);

        // 删除旧Token（一次性消费）
        tokenCacheHelper.deleteTokens(request.getUserKey());

        // 使用Helper构建响应
        return tokenGenerateHelper.buildTokenResponse(newAccessToken, newRefreshToken,
            deviceType, accessTokenExpire, refreshTokenExpire);
    }

    @Override
    public boolean support(String deviceType) {
        return DeviceType.PC.name().equals(deviceType) || DeviceType.UNKNOWN.name().equals(deviceType);
    }
}

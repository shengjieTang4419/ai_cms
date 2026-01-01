package com.cloud.common.security.service;


import com.cloud.common.core.constant.CacheConstants;
import com.cloud.common.core.constant.DeviceType;
import com.cloud.common.core.constant.SecurityConstants;
import com.cloud.common.core.constant.TokenStrategy;
import com.cloud.common.core.exception.ServiceException;
import com.cloud.common.core.util.IpUtils;
import com.cloud.common.core.util.JwtUtils;
import com.cloud.common.core.util.StringUtils;
import com.cloud.common.core.uuid.IdUtils;
import com.cloud.common.security.dto.TokenRefreshRequest;
import com.cloud.common.security.helper.DeviceSecurityHelper;
import com.cloud.common.security.helper.TokenCacheHelper;
import com.cloud.common.security.helper.TokenGenerateHelper;
import com.cloud.common.security.strategy.TokenRefreshStrategy;
import com.cloud.system.api.dto.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:token服务
 * @date 2025/12/20 15:43
 */
@Component
@RequiredArgsConstructor
public class TokenService {

    private final RedissonClient redisson;
    private final List<TokenRefreshStrategy> refreshStrategies;
    private final TokenGenerateHelper tokenGenerateHelper;
    private final TokenCacheHelper tokenCacheHelper;

    /**
     * 创建Token（根据设备类型自动选择策略）
     */
    public Map<String, Object> createToken(LoginUser loginUser, String deviceId) {
        DeviceType deviceType = DeviceType.fromDeviceId(deviceId);
        return createTokenWithStrategy(loginUser, deviceId, deviceType);
    }

    /**
     * 创建Token（指定策略）
     */
    private Map<String, Object> createTokenWithStrategy(LoginUser loginUser, String deviceId, DeviceType deviceType) {
        long accessTokenExpire = TokenStrategy.getAccessTokenExpire(deviceType);
        long refreshTokenExpire = TokenStrategy.getRefreshTokenExpire(deviceType);
        String userKey = IdUtils.fastUUID();
        Long userId = loginUser.getUserId();
        String userName = loginUser.getUserName();
        String ipAddr = IpUtils.getIpAddr();

        // 使用Helper生成Token
        String accessToken = tokenGenerateHelper.generateAccessToken(userKey, userId, userName);
        String refreshToken = tokenGenerateHelper.generateRefreshToken(userKey, userId, userName);
        // 使用Helper缓存Token
        tokenCacheHelper.cacheAccessToken(userKey, accessToken, userId, userName,
                deviceId, deviceType, ipAddr, accessTokenExpire);
        tokenCacheHelper.cacheRefreshToken(userKey, refreshToken, userId, userName,
                deviceId, deviceType, ipAddr, refreshTokenExpire);

        // 使用Helper构建响应
        return tokenGenerateHelper.buildTokenResponse(accessToken, refreshToken,
                deviceType, accessTokenExpire, refreshTokenExpire);
    }

    /**
     * 刷新Token（策略模式，增强设备安全校验）
     */
    public Map<String, Object> refreshToken(String refreshToken, String currentDeviceId, HttpServletRequest request) {
        try {
            // 0. 可疑请求检查（额外安全层）
            if (DeviceSecurityHelper.isSuspiciousRequest(request)) {
                throw new BadCredentialsException("检测到可疑请求，拒绝访问");
            }

            // 1. JWT基础验证
            Claims claims = JwtUtils.parseRefreshToken(refreshToken);
            if (claims == null || !SecurityConstants.REFRESH_TOKEN_TYPE.equals(claims.get(SecurityConstants.TOKEN_TYPE))) {
                throw new BadCredentialsException("RefreshToken无效或类型错误");
            }

            // 2. 提取关键信息
            String userKey = (String) claims.get(SecurityConstants.USER_KEY);
            Long userId = (Long) claims.get(SecurityConstants.DETAILS_USER_ID);
            String username = (String) claims.get(SecurityConstants.DETAILS_USERNAME);
            Long issuedAt = claims.getIssuedAt().getTime();

            // 3. Redis状态验证（从RefreshToken存储中获取）
            String refreshRedisKey = CacheConstants.REFRESH_TOKEN_KEY + userKey;
            RBucket<Map<String, Object>> refreshBucket = redisson.getBucket(refreshRedisKey);

            if (!refreshBucket.isExists()) {
                throw new BadCredentialsException("RefreshToken已过期或已被使用");
            }

            Map<String, Object> refreshTokenInfo = refreshBucket.get();

            // 4. 获取设备类型
            String cachedDeviceType = (String) refreshTokenInfo.get("deviceType");
            DeviceType deviceType = cachedDeviceType != null ?
                    DeviceType.valueOf(cachedDeviceType.toUpperCase()) : DeviceType.fromDeviceId(currentDeviceId);

            // 5. 失效窗口校验（根据设备类型）
            if (!isInRefreshWindow(issuedAt, deviceType)) {
                throw new BadCredentialsException("RefreshToken不在刷新时间窗口内");
            }

            // 6. 验证用户信息一致性
            if (!userId.equals(refreshTokenInfo.get("userId")) ||
                    !username.equals(refreshTokenInfo.get("username"))) {
                throw new BadCredentialsException("RefreshToken信息不匹配");
            }

            // 7. 严格设备信息验证（使用设备指纹校验）
            String cachedDeviceId = (String) refreshTokenInfo.get("deviceId");
            if (!DeviceSecurityHelper.isDeviceMatchStrict(cachedDeviceId, request)) {
                throw new BadCredentialsException("检测到设备变化，请重新登录");
            }

            // 8. 构建刷新请求参数
            TokenRefreshRequest tokenRequest = TokenRefreshRequest.builder()
                    .userKey(userKey)
                    .userId(userId)
                    .username(username)
                    .deviceId(currentDeviceId)
                    .deviceType(deviceType)
                    .oldTokenInfo(refreshTokenInfo)
                    .currentIp(IpUtils.getIpAddr())
                    .build();

            // 9. 使用策略模式刷新Token
            TokenRefreshStrategy strategy = getRefreshStrategy(deviceType);
            return strategy.refreshToken(tokenRequest);

        } catch (Exception e) {
            throw new ServiceException("刷新Token失败: " + e.getMessage());
        }
    }

    /**
     * 获取刷新策略
     */
    private TokenRefreshStrategy getRefreshStrategy(DeviceType deviceType) {
        return refreshStrategies.stream()
                .filter(strategy -> strategy.support(deviceType.name()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到对应的刷新策略: " + deviceType));
    }

    /**
     * 验证设备信息是否发生变化（已废弃，使用DeviceSecurityHelper替代）
     *
     * @param cachedDeviceId  缓存中的设备ID
     * @param currentDeviceId 当前设备ID
     * @return true表示设备未变化，false表示设备已变化
     * @deprecated 使用 {@link DeviceSecurityHelper#isDeviceMatchStrict(String, HttpServletRequest)} 替代
     */
    @Deprecated
    private boolean isDeviceChanged(String cachedDeviceId, String currentDeviceId) {
        // 如果都没有设备信息，认为未变化
        if (StringUtils.isEmpty(cachedDeviceId) && StringUtils.isEmpty(currentDeviceId)) {
            return false;
        }
        // 其中一个有设备信息，认为设备变化
        if (StringUtils.isEmpty(cachedDeviceId) || StringUtils.isEmpty(currentDeviceId)) {
            return true;
        }
        // 设备ID不同，认为设备变化
        return !cachedDeviceId.equals(currentDeviceId);
    }

    /**
     * 检查是否在刷新窗口内（根据设备类型动态判断）
     */
    private boolean isInRefreshWindow(Long issuedAt, DeviceType deviceType) {
        //移动端没有刷新窗口概念
        if (DeviceType.MOBILE.equals(deviceType)) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long tokenAge = currentTime - issuedAt;

        // 根据设备类型获取对应的策略
        long totalLifetime = TokenStrategy.getRefreshTokenExpire(deviceType) * 60 * 1000L;
        double windowThreshold = TokenStrategy.getRefreshWindowThreshold(deviceType);

        // 计算刷新窗口
        double minRefreshTime = totalLifetime * windowThreshold;
        return tokenAge >= minRefreshTime && tokenAge < totalLifetime;
    }


    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userKey = JwtUtils.getUserKey(token);
            tokenCacheHelper.deleteTokens(userKey);
        }
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(String token) {
        LoginUser user = null;
        if (StringUtils.isNotEmpty(token)) {
            String userKey = JwtUtils.getUserKey(token);
            RBucket<LoginUser> userRBucket = redisson.getBucket(CacheConstants.LOGIN_TOKEN_KEY + userKey);
            user = userRBucket.get();
            return user;
        }

        return user;
    }

}

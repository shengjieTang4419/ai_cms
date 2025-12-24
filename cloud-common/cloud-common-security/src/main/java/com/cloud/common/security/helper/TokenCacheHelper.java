package com.cloud.common.security.helper;

import com.cloud.common.core.constant.CacheConstants;
import com.cloud.common.core.constant.DeviceType;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Token缓存帮助类
 *
 * @author shengjie.tang
 */
@Component
@RequiredArgsConstructor
public class TokenCacheHelper {

    private final RedissonClient redisson;

    /**
     * 通用缓存方法
     *
     * @param keyPrefix  Redis key前缀
     * @param userKey    用户key
     * @param tokenInfo  Token信息
     * @param ttlMinutes TTL（分钟）
     */
    public void cacheToken(String keyPrefix, String userKey, Map<String, Object> tokenInfo, long ttlMinutes) {
        String redisKey = keyPrefix + userKey;
        RBucket<Map<String, Object>> bucket = redisson.getBucket(redisKey);
        bucket.set(tokenInfo, Duration.ofMinutes(ttlMinutes));
    }

    /**
     * 缓存AccessToken信息
     */
    public void cacheAccessToken(String userKey, String accessToken, Integer userId, String username,
                                 String deviceId, DeviceType deviceType, String ipAddr, long accessTokenExpire) {
        Map<String, Object> accessInfo = new HashMap<>();
        accessInfo.put("accessToken", accessToken);
        accessInfo.put("accessIpAddr", ipAddr);
        accessInfo.put("accessTokenExpire", accessTokenExpire);
        buildOtherAttrs(accessInfo, userKey, userId, username, deviceId, deviceType);
        cacheToken(CacheConstants.LOGIN_TOKEN_KEY, userKey, accessInfo, accessTokenExpire);
    }


    private void buildOtherAttrs(Map<String, Object> tokenMap, String userKey, Integer userId, String username,
                                 String deviceId, DeviceType deviceType) {
        tokenMap.put("userKey", userKey);
        tokenMap.put("userId", userId);
        tokenMap.put("username", username);
        tokenMap.put("deviceId", deviceId);
        tokenMap.put("deviceType", deviceType.name());
        tokenMap.put("createTime", System.currentTimeMillis());
    }

    /**
     * 缓存RefreshToken信息
     */
    public void cacheRefreshToken(String userKey, String refreshToken, Integer userId, String username,
                                  String deviceId, DeviceType deviceType, String ipAddr, long refreshTokenExpire) {
        Map<String, Object> refreshInfo = new HashMap<>();
        refreshInfo.put("refreshToken", refreshToken);
        refreshInfo.put("refreshIpAddr", ipAddr);
        buildOtherAttrs(refreshInfo, userKey, userId, username, deviceId, deviceType);
        cacheToken(CacheConstants.REFRESH_TOKEN_KEY, userKey, refreshInfo, refreshTokenExpire);
    }

    /**
     * 更新RefreshToken信息（保持原TTL）
     */
    public void updateRefreshToken(String userKey, String newIp) {
        String refreshKey = CacheConstants.REFRESH_TOKEN_KEY + userKey;
        RBucket<Map<String, Object>> refreshBucket = redisson.getBucket(refreshKey);

        if (refreshBucket.isExists()) {
            long remainingTtl = refreshBucket.remainTimeToLive();
            Map<String, Object> refreshInfo = refreshBucket.get();
            refreshInfo.put("refreshIpAddr", newIp);
            refreshInfo.put("lastRefreshTime", System.currentTimeMillis());

            if (remainingTtl > 0) {
                refreshBucket.set(refreshInfo, Duration.ofMillis(remainingTtl));
            }
        }
    }

    /**
     * 删除Token
     */
    public void deleteTokens(String userKey) {
        redisson.getBucket(CacheConstants.LOGIN_TOKEN_KEY + userKey).delete();
        redisson.getBucket(CacheConstants.REFRESH_TOKEN_KEY + userKey).delete();
    }
}

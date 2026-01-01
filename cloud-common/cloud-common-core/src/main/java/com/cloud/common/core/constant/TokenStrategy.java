package com.cloud.common.core.constant;

/**
 * Token策略工具类 - 根据设备类型返回对应的Token配置
 *
 * @author shengjie.tang
 */
public class TokenStrategy {
    
    /**
     * 获取AccessToken过期时间（分钟）
     */
    public static long getAccessTokenExpire(DeviceType deviceType) {
        if (deviceType.isMobile()) {
            return CacheConstants.MOBILE_ACCESS_TOKEN_EXPIRE;
        }
        return CacheConstants.PC_ACCESS_TOKEN_EXPIRE;
    }
    
    /**
     * 获取RefreshToken过期时间（分钟）
     */
    public static long getRefreshTokenExpire(DeviceType deviceType) {
        if (deviceType.isMobile()) {
            return CacheConstants.MOBILE_REFRESH_TOKEN_EXPIRE;
        }
        return CacheConstants.PC_REFRESH_TOKEN_EXPIRE;
    }
    
    /**
     * 获取Token刷新阈值
     */
    public static double getRefreshThreshold(DeviceType deviceType) {
        if (deviceType.isMobile()) {
            return CacheConstants.MOBILE_REFRESH_THRESHOLD;
        }
        return CacheConstants.PC_REFRESH_THRESHOLD;
    }
    
    /**
     * 获取RefreshToken刷新窗口阈值
     */
    public static double getRefreshWindowThreshold(DeviceType deviceType) {
        if (deviceType.isMobile()) {
            return CacheConstants.MOBILE_REFRESH_WINDOW_THRESHOLD;
        }
        return CacheConstants.PC_REFRESH_WINDOW_THRESHOLD;
    }
    
    /**
     * 获取策略描述
     */
    public static String getStrategyDescription(DeviceType deviceType) {
        if (deviceType.isMobile()) {
            return String.format("移动端策略: AccessToken=%d分钟, RefreshToken=%d天", 
                CacheConstants.MOBILE_ACCESS_TOKEN_EXPIRE,
                CacheConstants.MOBILE_REFRESH_TOKEN_EXPIRE / (24 * 60));
        }
        return String.format("PC端策略: AccessToken=%d分钟, RefreshToken=%d分钟", 
            CacheConstants.PC_ACCESS_TOKEN_EXPIRE,
            CacheConstants.PC_REFRESH_TOKEN_EXPIRE);
    }
}

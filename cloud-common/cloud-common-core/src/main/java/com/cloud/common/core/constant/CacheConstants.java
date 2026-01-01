package com.cloud.common.core.constant;

/**
 * 缓存常量信息
 *
 * @author shengjie.tang
 */
public class CacheConstants {
    /**
     * ==================== PC端Token策略（安全优先） ====================
     */

    /**
     * PC端 - AccessToken有效期，默认60（分钟）
     */
    public final static long PC_ACCESS_TOKEN_EXPIRE = 60;

    /**
     * PC端 - RefreshToken有效期，默认60（分钟）- 一次性消费
     */
    public final static long PC_REFRESH_TOKEN_EXPIRE = 60;

    /**
     * PC端 - Token刷新阈值，3/4有效期时刷新
     */
    public final static double PC_REFRESH_THRESHOLD = 0.75;

    /**
     * PC端 - RefreshToken刷新窗口，45分钟后才能刷新（45-60分钟窗口）
     */
    public final static double PC_REFRESH_WINDOW_THRESHOLD = 0.75;

    /**
     * ==================== 移动端Token策略（体验优先） ====================
     */

    /**
     * 移动端 - AccessToken有效期，默认120（分钟）
     */
    public final static long MOBILE_ACCESS_TOKEN_EXPIRE = 120;

    /**
     * 移动端 - RefreshToken有效期，默认7天
     */
    public final static long MOBILE_REFRESH_TOKEN_EXPIRE = 7 * 24 * 60;

    /**
     * 移动端 - Token刷新阈值，3/4有效期时刷新
     */
    public final static double MOBILE_REFRESH_THRESHOLD = 0.75;

    /**
     * 移动端 - RefreshToken刷新窗口，90分钟后才能刷新（90-120分钟窗口）
     */
    public final static double MOBILE_REFRESH_WINDOW_THRESHOLD = 0.75;

    /**
     * ==================== 通用配置（兼容旧代码） ====================
     */

    /**
     * 缓存有效期，默认60（分钟）- 默认使用PC端策略
     */
    public final static long EXPIRATION = PC_ACCESS_TOKEN_EXPIRE;

    /**
     * ==================== 其他配置 ====================
     */

    /**
     * 密码最大错误次数
     */
    public final static int PASSWORD_MAX_RETRY_COUNT = 5;

    /**
     * 密码锁定时间，默认10（分钟）
     */
    public final static long PASSWORD_LOCK_TIME = 10;

    /**
     * 权限缓存前缀
     */
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * Refresh Token缓存前缀
     */
    public final static String REFRESH_TOKEN_KEY = "refresh_tokens:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 登录IP黑名单 cache key
     */
    public static final String SYS_LOGIN_BLACKIPLIST = SYS_CONFIG_KEY + "sys.login.blackIPList";
}

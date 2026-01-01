package com.cloud.common.core.constant;

/**
 * Token的Key常量
 *
 * @author ruoyi
 */
public class TokenConstants {
    /**
     * 令牌自定义标识
     */
    public static final String AUTHENTICATION = "Authorization";

    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

    /**
     * 令牌秘钥 - HS512算法需要至少64字节
     */
    public final static String SECRET = "mySecretKeyForJWTTokenGenerationThatShouldBeLongEnoughForHS512AlgorithmAndMustBeAtLeast64BytesLong";

    /**
     * 刷新令牌秘钥 - HS512算法需要至少64字节
     */
    public final static String REFRESH_SECRET = "myRefreshSecretKeyForJWTTokenGenerationThatShouldBeLongEnoughForHS512AlgorithmAndMustBeAtLeast64BytesLong";

}

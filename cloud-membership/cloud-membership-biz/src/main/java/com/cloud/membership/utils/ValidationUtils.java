package com.cloud.membership.utils;

import java.math.BigDecimal;

/**
 * 数据验证工具类
 *
 * @author AI Assistant
 * @since 1.0.0
 */
public class ValidationUtils {

    private static final int MAX_TAG_NAME_LENGTH = 50;
    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(1000);
    private static final int MAX_RECOMMENDATION_LIMIT = 10;

    /**
     * 验证标签名称
     *
     * @param tagName 标签名称
     * @throws IllegalArgumentException 如果标签名称无效
     */
    public static void validateTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }

        String trimmedTagName = tagName.trim();
        if (trimmedTagName.length() > MAX_TAG_NAME_LENGTH) {
            throw new IllegalArgumentException("标签名称长度不能超过" + MAX_TAG_NAME_LENGTH + "个字符");
        }

        // 检查是否包含非法字符
        if (trimmedTagName.matches(".*[<>\"'&].*")) {
            throw new IllegalArgumentException("标签名称包含非法字符");
        }

        // 检查是否只包含空白字符
        if (trimmedTagName.matches("\\s+")) {
            throw new IllegalArgumentException("标签名称不能只包含空白字符");
        }
    }

    /**
     * 验证权重值
     *
     * @param weight 权重值
     * @throws IllegalArgumentException 如果权重值无效
     */
    public static void validateWeight(BigDecimal weight) {
        if (weight == null) {
            throw new IllegalArgumentException("权重不能为空");
        }

        if (weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("权重不能为负数");
        }

        if (weight.compareTo(MAX_WEIGHT) > 0) {
            throw new IllegalArgumentException("权重不能超过" + MAX_WEIGHT);
        }
    }

    /**
     * 验证推荐数量参数
     *
     * @param limit 推荐数量
     * @throws IllegalArgumentException 如果参数无效
     */
    public static void validateRecommendationLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("推荐数量必须为正数");
        }

        if (limit > MAX_RECOMMENDATION_LIMIT) {
            throw new IllegalArgumentException("推荐数量不能超过" + MAX_RECOMMENDATION_LIMIT);
        }
    }

    /**
     * 验证推荐数量参数（带默认值）
     *
     * @param limit        推荐数量
     * @param defaultValue 默认值
     * @return 验证后的推荐数量
     */
    public static Integer validateRecommendationLimitWithDefault(Integer limit, Integer defaultValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }

        if (limit > MAX_RECOMMENDATION_LIMIT) {
            throw new IllegalArgumentException("推荐数量不能超过" + MAX_RECOMMENDATION_LIMIT);
        }

        return limit;
    }

    /**
     * 清理标签名称（去除前后空白字符）
     *
     * @param tagName 原始标签名称
     * @return 清理后的标签名称
     */
    public static String cleanTagName(String tagName) {
        if (tagName == null) {
            return null;
        }
        return tagName.trim();
    }
}

package com.cloud.membership.constants;

/**
 * 用户画像算法常量
 */
public final class AlgorithmConstants {

    private AlgorithmConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 标签相关常量
     */
    public static final class Tag {
        /**
         * 最大标签数量，超过则触发竞争淘汰
         */
        public static final int MAX_TAGS = 50;

        /**
         * 保护区大小（TopK中的稳定标签）
         */
        public static final int PROTECTED_SIZE = 20;

        /**
         * 权重因子：70%权重 + 30%新鲜度
         */
        public static final double WEIGHT_FACTOR = 0.7;
        public static final double FRESHNESS_FACTOR = 0.3;

        /**
         * 新鲜度半衰期：7天
         */
        public static final long FRESHNESS_HALF_LIFE_MS = 7L * 24 * 60 * 60 * 1000;
    }

    /**
     * 坍缩相关常量
     */
    public static final class Collapse {
        /**
         * 坍缩基础阈值（参考 CAS 自旋思想）
         */
        public static final int BASE_THRESHOLD = 10;

        /**
         * 坍缩年龄因子（类似 JVM 分代年龄）
         */
        public static final double AGE_FACTOR = 0.5;
    }

    /**
     * 发散相关常量
     */
    public static final class Divergence {
        /**
         * 低置信度阈值：用于判断是否需要快速发散
         */
        public static final double LOW_CONFIDENCE_THRESHOLD = 0.3;

        /**
         * 刻板印象匹配度阈值：70% 以上认为匹配
         */
        public static final double STEREOTYPE_MATCH_THRESHOLD = 0.7;

        /**
         * 对话补偿触达次数：仅在前N次有效对话尝试快速模版匹配
         */
        public static final int CHAT_TOP_K = 5;
    }
}

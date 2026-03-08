package com.cloud.membership.service.impl;


import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 衰减机制
 * @date 2026/1/17 16:39
 */
@Service
public class AttenuationService {

    /**
     * 自然时间衰减
     * 所有标签都会随时间自然衰减，防止历史高分标签永久霸榜
     * 每30天衰减25%（0.75）
     */
    public double calculateNaturalDecay(double currentWeight, Object createdTime) {
        long now = System.currentTimeMillis();
        long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

        long createdTimeMs = getTimestampMillis(createdTime, now);
        long tagAge = now - createdTimeMs;

        // 每30天衰减25%（0.75）
        double monthsAge = (double) tagAge / THIRTY_DAYS_MS;
        double naturalDecay = Math.pow(0.75, monthsAge);

        return currentWeight * naturalDecay;
    }

    /**
     * 活跃度衰减
     * 7天内的标签不衰减，7天后开始半衰减
     * 每7天衰减50%
     */
    public double calculateActivityDecay(double currentWeight, Object lastActiveTime) {
        long now = System.currentTimeMillis();
        long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

        long lastActiveTimeMs = getTimestampMillis(lastActiveTime, now);
        long timeSinceActive = now - lastActiveTimeMs;

        double activityDecay = 1.0;
        if (timeSinceActive > SEVEN_DAYS_MS) {
            // 每7天衰减50%
            double daysSinceActive = (double) timeSinceActive / SEVEN_DAYS_MS;
            activityDecay = Math.pow(0.5, daysSinceActive);
        }

        return currentWeight * activityDecay;
    }

    /**
     * 计算衰减后的权重（保留原有方法用于兼容）
     * 双重衰减机制：活跃度衰减 + 自然时间衰减
     */
    public double calculateDecayedWeight(double currentWeight, Object lastActiveTime, Object createdTime) {
        double activityDecayed = calculateActivityDecay(currentWeight, lastActiveTime);
        return calculateNaturalDecay(activityDecayed, createdTime);
    }

    /**
     * 统一处理时间戳转换（支持 LocalDateTime 和 Date）
     */
    private long getTimestampMillis(Object timeObj, long defaultValue) {
        if (timeObj == null) {
            return defaultValue;
        }
        if (timeObj instanceof LocalDateTime) {
            return Timestamp.valueOf((java.time.LocalDateTime) timeObj).getTime();
        }
        if (timeObj instanceof Date) {
            return ((Date) timeObj).getTime();
        }
        return defaultValue;
    }
}

package com.cloud.common.core.util;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 设备指纹提取器 - 核心设备识别逻辑
 * 职责：从User-Agent中提取设备特征，生成设备指纹
 */
@Slf4j
public class DeviceFingerprintExtractor {

    /**
     * 从User-Agent中提取设备特征（移动端）
     */
    public static String extractDeviceFingerprintFromUserAgent(String userAgent) {
        if (StringUtils.isEmpty(userAgent)) {
            return null;
        }

        // iPhone设备识别
        if (userAgent.contains("iPhone")) {
            // 提取iPhone型号和iOS版本
            String model = extractiPhoneModel(userAgent);
            String osVersion = extractiOSVersion(userAgent);
            return "iPhone_" + model + "_" + osVersion;
        }

        // Android设备识别
        if (userAgent.contains("Android")) {
            // 提取Android设备型号和版本
            String model = extractAndroidModel(userAgent);
            String version = extractAndroidVersion(userAgent);
            return "Android_" + model + "_" + version;
        }

        // iPad设备识别
        if (userAgent.contains("iPad")) {
            String model = extractiPadModel(userAgent);
            String osVersion = extractiOSVersion(userAgent);
            return "iPad_" + model + "_" + osVersion;
        }

        return null;
    }

    /**
     * 提取iPhone型号
     */
    public static String extractiPhoneModel(String userAgent) {
        if (userAgent.contains("iPhone14,1")) return "14_Pro";
        if (userAgent.contains("iPhone14,2")) return "14_Pro_Max";
        if (userAgent.contains("iPhone14,3")) return "15_Pro";
        if (userAgent.contains("iPhone14,4")) return "15_Pro_Max";
        if (userAgent.contains("iPhone14,5")) return "14";
        if (userAgent.contains("iPhone14,6")) return "14_Plus";
        if (userAgent.contains("iPhone14,7")) return "15";
        if (userAgent.contains("iPhone14,8")) return "15_Plus";
        if (userAgent.contains("iPhone13,")) return "13_Series";
        if (userAgent.contains("iPhone12,")) return "12_Series";
        return "Unknown";
    }

    /**
     * 提取iPad型号
     */
    public static String extractiPadModel(String userAgent) {
        if (userAgent.contains("iPad14,1") || userAgent.contains("iPad14,2")) return "iPad_Mini_6";
        if (userAgent.contains("iPad13,")) return "iPad_Pro_3";
        if (userAgent.contains("iPad12,")) return "iPad_Pro_2";
        if (userAgent.contains("iPad11,")) return "iPad_Pro_1";
        return "Unknown";
    }

    /**
     * 提取iOS版本
     */
    public static String extractiOSVersion(String userAgent) {
        int start = userAgent.indexOf("CPU OS ");
        if (start != -1) {
            start += 7; // "CPU OS " length
            int end = userAgent.indexOf(" ", start);
            if (end == -1) end = userAgent.indexOf(";", start);
            if (end != -1) {
                return userAgent.substring(start, end).replace("_", ".");
            }
        }
        return "Unknown";
    }

    /**
     * 提取Android设备型号
     */
    public static String extractAndroidModel(String userAgent) {
        int start = userAgent.indexOf("; ");
        if (start != -1) {
            start += 2;
            int end = userAgent.indexOf(")", start);
            if (end != -1) {
                String model = userAgent.substring(start, end);
                // 清理常见的前缀
                model = model.replace("Build/", "").replace(" ", "_");
                return model.length() > 20 ? model.substring(0, 20) : model;
            }
        }
        return "Unknown";
    }

    /**
     * 提取Android版本
     */
    public static String extractAndroidVersion(String userAgent) {
        int start = userAgent.indexOf("Android ");
        if (start != -1) {
            start += 8; // "Android " length
            int end = userAgent.indexOf(";", start);
            if (end == -1) end = userAgent.indexOf(" ", start);
            if (end != -1) {
                return userAgent.substring(start, end);
            }
        }
        return "Unknown";
    }

    /**
     * 生成User-Agent哈希（桌面端降级方案）
     */
    public static String generateUserAgentHash(String userAgent) {
        if (StringUtils.isEmpty(userAgent)) {
            return "Unknown_Device";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(userAgent.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return "Desktop_" + sb.substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            log.warn("MD5 algorithm not available", e);
            return "Desktop_Fallback";
        }
    }

    /**
     * 生成设备ID的通用逻辑
     * 优先级1: 设备ID -> 优先级2: 设备指纹 -> 优先级3: User-Agent哈希
     */
    public static String generateDeviceId(String deviceId, String userAgent) {
        // 优先级1: 使用设备ID
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            return deviceId.trim();
        }

        // 优先级2: 从User-Agent提取设备指纹
        String deviceFingerprint = extractDeviceFingerprintFromUserAgent(userAgent);
        if (deviceFingerprint != null && !deviceFingerprint.trim().isEmpty()) {
            return deviceFingerprint;
        }

        // 优先级3: 降级到User-Agent哈希
        return generateUserAgentHash(userAgent);
    }
}

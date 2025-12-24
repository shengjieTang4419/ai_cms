package com.cloud.common.core.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 设备信息工具类 - Servlet环境
 * 职责：从HttpServletRequest中提取设备信息
 */
public class DeviceUtils {

    /**
     * 从HTTP请求中提取设备信息
     */
    public static String extractDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 获取设备ID和User-Agent
        String deviceId = request.getHeader("X-Device-ID");
        String userAgent = request.getHeader("User-Agent");

        // 使用核心工具类生成设备ID
        return DeviceFingerprintExtractor.generateDeviceId(deviceId, userAgent);
    }

    /**
     * 验证设备信息是否匹配
     */
    public static boolean isDeviceMatch(String cachedDeviceId, HttpServletRequest request) {
        if (StringUtils.isEmpty(cachedDeviceId)) {
            // 如果缓存中没有设备信息，允许通过（兼容旧版本）
            return true;
        }

        String currentDeviceId = extractDeviceId(request);
        if (StringUtils.isEmpty(currentDeviceId)) {
            // 如果当前请求无法获取设备信息，拒绝访问
            return false;
        }

        boolean matched = cachedDeviceId.equals(currentDeviceId);
        return matched;
    }
}

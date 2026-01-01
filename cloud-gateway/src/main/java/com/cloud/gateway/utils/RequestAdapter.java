package com.cloud.gateway.utils;

import com.cloud.common.core.util.DeviceFingerprintExtractor;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 请求适配器 - 网关环境适配器
 * 职责：将ServerHttpRequest适配为设备验证可用的格式
 */
public class RequestAdapter {

    /**
     * 从ServerHttpRequest中提取设备ID
     */
    public static String extractDeviceId(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }

        // 获取设备ID和User-Agent
        String deviceId = request.getHeaders().getFirst("X-Device-ID");
        String userAgent = request.getHeaders().getFirst("User-Agent");

        // 使用核心工具类生成设备ID
        return DeviceFingerprintExtractor.generateDeviceId(deviceId, userAgent);
    }

    /**
     * 验证设备信息是否匹配
     */
    public static boolean isDeviceMatch(String cachedDeviceId, ServerHttpRequest request) {
        if (cachedDeviceId == null || cachedDeviceId.trim().isEmpty()) {
            // 如果缓存中没有设备信息，允许通过（兼容旧版本）
            return true;
        }

        String currentDeviceId = extractDeviceId(request);
        if (currentDeviceId == null || currentDeviceId.trim().isEmpty()) {
            // 如果当前请求无法获取设备信息，拒绝访问
            return false;
        }

        boolean matched = cachedDeviceId.equals(currentDeviceId);
        return matched;
    }

    /**
     * 获取客户端真实IP（用于日志记录）
     */
    public static String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }

        ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeaders().getFirst("Proxy-Client-IP");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 获取远程地址
        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}

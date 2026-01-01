package com.cloud.common.security.helper;

import com.cloud.common.core.util.DeviceFingerprintExtractor;
import com.cloud.common.core.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 设备安全校验助手类
 * 职责：在Token刷新时进行严格的设备指纹校验
 * 
 * @author shengjie.tang
 */
public class DeviceSecurityHelper {

    /**
     * 严格校验设备信息是否匹配
     * 用于Token刷新时的安全验证
     * 
     * @param cachedDeviceId 缓存中的设备ID
     * @param request 当前请求
     * @return true表示设备匹配，false表示设备不匹配
     */
    public static boolean isDeviceMatchStrict(String cachedDeviceId, HttpServletRequest request) {
        // 1. 基础空值检查
        if (StringUtils.isEmpty(cachedDeviceId)) {
            // 如果缓存中没有设备信息，允许通过（兼容旧版本）
            return true;
        }

        if (request == null) {
            // 请求对象为空，拒绝访问
            return false;
        }

        // 2. 提取当前请求的设备信息
        String currentDeviceId = extractDeviceId(request);
        if (StringUtils.isEmpty(currentDeviceId)) {
            // 当前请求无法获取设备信息，拒绝访问
            return false;
        }

        // 3. 严格匹配设备ID
        boolean isExactMatch = cachedDeviceId.equals(currentDeviceId);
        
        // 4. 记录校验日志（用于安全审计）
        if (!isExactMatch) {
            securityLog(cachedDeviceId, currentDeviceId, request);
        }
        
        return isExactMatch;
    }

    /**
     * 从HttpServletRequest中提取设备ID
     * 使用与DeviceUtils相同的逻辑，但增加安全校验
     */
    private static String extractDeviceId(HttpServletRequest request) {
        // 获取设备ID和User-Agent
        String deviceId = request.getHeader("X-Device-ID");
        String userAgent = request.getHeader("User-Agent");

        // 安全检查：防止头部注入攻击
        if (deviceId != null && deviceId.length() > 200) {
            return null; // 设备ID过长，可能是攻击
        }

        if (userAgent != null && userAgent.length() > 500) {
            return null; // User-Agent过长，可能是攻击
        }

        // 使用核心工具类生成设备ID
        return DeviceFingerprintExtractor.generateDeviceId(deviceId, userAgent);
    }

    /**
     * 设备变化安全日志
     * 记录潜在的Token劫持尝试
     */
    private static void securityLog(String cachedDeviceId, String currentDeviceId, HttpServletRequest request) {
        // 这里可以集成日志系统，记录安全事件
        // 例如：发送到安全监控平台、记录到安全日志表等
        
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        System.err.println("=== 设备安全校验失败 ===");
        System.err.println("时间: " + java.time.LocalDateTime.now());
        System.err.println("客户端IP: " + clientIp);
        System.err.println("缓存设备ID: " + cachedDeviceId);
        System.err.println("当前设备ID: " + currentDeviceId);
        System.err.println("User-Agent: " + userAgent);
        System.err.println("========================");
        
        // TODO: 集成实际的安全监控系统
        // securityEventService.logDeviceMismatch(cachedDeviceId, currentDeviceId, clientIp, userAgent);
    }

    /**
     * 获取客户端真实IP
     */
    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查是否为可疑请求
     * 用于额外的安全检查
     */
    public static boolean isSuspiciousRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        
        // 检查常见的攻击工具特征
        if (userAgent != null) {
            String lowerUserAgent = userAgent.toLowerCase();
            return lowerUserAgent.contains("sqlmap") ||
                   lowerUserAgent.contains("nmap") ||
                   lowerUserAgent.contains("nikto") ||
                   lowerUserAgent.contains("burp") ||
                   lowerUserAgent.contains("metasploit");
        }
        
        return false;
    }
}

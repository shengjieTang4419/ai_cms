package com.cloud.common.core.constant;

/**
 * 设备类型枚举
 *
 * @author shengjie.tang
 */
public enum DeviceType {
    
    /**
     * PC端（Web浏览器）
     */
    PC("pc", "PC端"),
    
    /**
     * 移动端（iOS/Android APP）
     */
    MOBILE("mobile", "移动端"),
    
    /**
     * 未知设备（默认使用PC策略）
     */
    UNKNOWN("unknown", "未知设备");
    
    private final String code;
    private final String desc;
    
    DeviceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据设备ID判断设备类型
     */
    public static DeviceType fromDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return UNKNOWN;
        }
        
        // 移动端设备ID通常包含特定标识
        if (deviceId.startsWith("mobile_") || 
            deviceId.contains("ios_") || 
            deviceId.contains("android_")) {
            return MOBILE;
        }
        
        // PC端设备ID
        if (deviceId.startsWith("pc_") || deviceId.startsWith("web_")) {
            return PC;
        }
        
        return UNKNOWN;
    }
    
    /**
     * 是否为移动端
     */
    public boolean isMobile() {
        return this == MOBILE;
    }
    
    /**
     * 是否为PC端
     */
    public boolean isPc() {
        return this == PC;
    }
}

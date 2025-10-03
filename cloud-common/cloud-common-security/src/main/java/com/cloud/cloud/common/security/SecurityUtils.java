package com.cloud.cloud.common.security;

import com.cloud.cloud.common.security.dto.CustomerUserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类
 * 用于获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        CustomerUserDetail currentUserDetail = getCurrentUserDetail();
        if (currentUserDetail == null) {
            return null;
        }
        return currentUserDetail.getUsername();
    }

    public static CustomerUserDetail getCurrentUserDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomerUserDetail) {
            return ((CustomerUserDetail) authentication.getPrincipal());
        }
        return null;
    }

    /**
     * 检查用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        CustomerUserDetail currentUserDetail = getCurrentUserDetail();
        if (currentUserDetail == null) {
            return null;
        }
        return currentUserDetail.getUserId();
    }
}

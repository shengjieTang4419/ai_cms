package com.cloud.common.security;

import com.cloud.common.core.constant.SecurityConstants;
import com.cloud.common.core.constant.TokenConstants;
import com.cloud.common.core.context.SecurityContextHolder;
import com.cloud.common.core.util.ServletUtils;
import com.cloud.common.core.util.StringUtils;
import com.cloud.system.api.dto.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

/**
 * Spring Security 工具类
 * 用于获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        return SecurityContextHolder.getUserName();
    }

    /**
     * 获取请求token
     */
    public static String getToken() {
        return getToken(Objects.requireNonNull(ServletUtils.getRequest()));
    }

    public static LoginUser getLoginUser() {
        return SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class);
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return
     */
    public static String getToken(HttpServletRequest request) {
        // 从header获取token标识
        String token = request.getHeader(TokenConstants.AUTHENTICATION);
        return replaceTokenPrefix(token);
    }

    /**
     * 裁剪token前缀
     */
    public static String replaceTokenPrefix(String token) {
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
            token = token.replaceFirst(TokenConstants.PREFIX, "");
        }
        return token;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return SecurityContextHolder.getUserId();
    }

    /**
     * 密码匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

}

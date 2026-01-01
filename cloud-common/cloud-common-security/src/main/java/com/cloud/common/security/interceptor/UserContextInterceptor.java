package com.cloud.common.security.interceptor;

import com.cloud.common.core.context.SecurityContextHolder;
import com.cloud.common.core.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 用户上下文拦截器
 * 从HTTP Header中读取用户信息并设置到SecurityContextHolder
 * 跨进程方案处理
 */
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_KEY_HEADER = "X-User-Key";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从Header中获取用户信息
        String userId = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String userKey = request.getHeader(USER_KEY_HEADER);

        // 设置到SecurityContextHolder
        if (StringUtils.hasText(userId)) {
            SecurityContextHolder.setUserId(userId);
        }
        if (StringUtils.hasText(username)) {
            // URL解码处理中文字符
            String decodedUsername = URLDecoder.decode(username, StandardCharsets.UTF_8);
            SecurityContextHolder.setUserName(decodedUsername);
        }
        if (StringUtils.hasText(userKey)) {
            SecurityContextHolder.setUserKey(userKey);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，防止内存泄漏
        SecurityContextHolder.remove();
    }
}

package com.cloud.common.security;


import com.cloud.common.security.dto.CustomerUserDetail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 简化的JWT认证过滤器 - 从Header获取用户信息
 * Gateway已验证Token，这里只需设置安全上下文
 */
@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    // 用户信息Header常量（与Gateway中保持一致）
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_KEY_HEADER = "X-User-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从Header获取用户信息（Gateway已验证）
            String userId = request.getHeader(USER_ID_HEADER);
            String username = request.getHeader(USERNAME_HEADER);
            String userKey = request.getHeader(USER_KEY_HEADER);

            if (StringUtils.hasText(userId) && StringUtils.hasText(username)) {
                // 设置安全上下文
                CustomerUserDetail userDetails = new CustomerUserDetail();
                try {
                    userDetails.setUserId(Long.valueOf(userId));
                } catch (NumberFormatException e) {
                    log.error("Invalid user ID format: {}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }
                userDetails.setUsername(username);
                userDetails.setUserKey(userKey);

                // 设置空权限列表，权限验证由具体业务服务处理
                userDetails.setAuthorities(Collections.emptyList());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}

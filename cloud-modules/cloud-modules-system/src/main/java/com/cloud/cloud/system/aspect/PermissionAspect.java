package com.cloud.cloud.system.aspect;

import com.cloud.cloud.common.security.SecurityUtils;
import com.cloud.cloud.system.annotation.RequirePermission;
import com.cloud.cloud.system.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 权限校验切面
 */
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private PermissionService permissionService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        // 检查是否需要登录
        if (requirePermission.requireAuth() && !SecurityUtils.isAuthenticated()) {
            throw new RuntimeException("未登录或登录已过期");
        }

        // 获取当前用户的角色和权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 这里需要从用户角色中获取权限列表
        String[] permissionCodes = requirePermission.value().split(",");

        for (String permissionCode : permissionCodes) {
            String code = permissionCode.trim();
            if (!StringUtils.isEmpty(code)) {
                // 检查权限是否存在且启用
                if (!permissionService.existsByPermissionCode(code)) {
                    throw new RuntimeException("权限不存在: " + code);
                }
                // 这里应该检查用户是否拥有该权限
                // 暂时跳过，实际实现需要从用户角色中获取权限
            }
        }

        return joinPoint.proceed();
    }
}

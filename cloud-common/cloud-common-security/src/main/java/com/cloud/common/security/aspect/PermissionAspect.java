package com.cloud.common.security.aspect;

import com.cloud.common.core.util.StringUtils;
import com.cloud.common.security.SecurityUtils;
import com.cloud.common.security.annotation.RequiresPermissions;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPermissions)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermissions requiresPermissions) {
        try {
            // 获取当前用户信息
            Long userId = SecurityUtils.getCurrentUserId();
            String username = SecurityUtils.getCurrentUsername();

            if (userId == null || StringUtils.isEmpty(username)) {
                throw new SecurityException("用户未登录");
            }

            // 这里可以根据需要实现具体的权限校验逻辑
            // 例如：调用System服务验证用户权限
            String[] permissions = requiresPermissions.value();
            if (permissions != null && permissions.length > 0) {
                boolean hasPermission = checkUserPermissions(userId, permissions, requiresPermissions.logical());
                if (!hasPermission) {
                    throw new SecurityException("权限不足");
                }
            }

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("权限校验失败: {}", e.getMessage());
            throw new SecurityException("权限校验失败");
        }
    }

    /**
     * 校验用户权限
     * 这里是示例实现，实际应该调用System服务的权限接口
     */
    private boolean checkUserPermissions(Long userId, String[] permissions, RequiresPermissions.Logical logical) {
        // 从缓存中获取用户权限 进行权限认证 后续实现

        // 示例：暂时返回true，实际应该根据业务逻辑实现
        log.debug("检查用户 {} 的权限: {}", userId, String.join(",", permissions));
        return true;
    }
}

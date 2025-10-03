package com.cloud.cloud.system.util;

import com.cloud.cloud.common.security.SecurityUtils;
import com.cloud.cloud.system.domain.Permission;
import com.cloud.cloud.system.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限工具类
 */
@Component
public class PermissionUtils {

    @Autowired
    private PermissionService permissionService;

    /**
     * 检查当前用户是否有指定权限
     */
    public boolean hasPermission(String permissionCode) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        // 这里需要从当前用户的角色中获取权限列表
        // 暂时简化实现，返回true
        // 实际实现需要：
        // 1. 获取当前用户的角色列表
        // 2. 获取每个角色的权限列表
        // 3. 检查是否包含指定权限
        return true;
    }

    /**
     * 检查当前用户是否有多个权限中的任意一个
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        for (String permissionCode : permissionCodes) {
            if (hasPermission(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否有多个权限中的全部
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        for (String permissionCode : permissionCodes) {
            if (!hasPermission(permissionCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前用户的所有权限代码
     */
    public List<String> getCurrentUserPermissions() {
        if (!SecurityUtils.isAuthenticated()) {
            return List.of();
        }

        // 这里需要从当前用户的角色中获取所有权限代码
        // 暂时返回空列表，实际实现需要业务模块提供用户角色信息
        return List.of();
    }

    /**
     * 检查权限是否存在且启用
     */
    public boolean isPermissionActive(String permissionCode) {
        return permissionService.findByPermissionCode(permissionCode)
                .map(Permission::getStatus)
                .map(status -> status == 1)
                .orElse(false);
    }
}

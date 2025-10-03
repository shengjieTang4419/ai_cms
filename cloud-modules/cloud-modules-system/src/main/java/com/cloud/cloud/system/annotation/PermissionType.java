package com.cloud.cloud.system.annotation;

/**
 * 权限类型枚举
 */
public enum PermissionType {
    /**
     * 与关系：必须拥有所有权限
     */
    AND,

    /**
     * 或关系：拥有任一权限即可
     */
    OR
}

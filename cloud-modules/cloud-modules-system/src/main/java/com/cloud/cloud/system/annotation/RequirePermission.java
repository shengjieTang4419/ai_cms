package com.cloud.cloud.system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 用于标注需要特定权限的方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限代码，如：USER_CREATE, CHAT_SEND
     */
    String value();

    /**
     * 权限类型：AND（必须全部拥有）, OR（拥有任一即可）
     */
    PermissionType type() default PermissionType.AND;

    /**
     * 是否需要登录（默认为true）
     */
    boolean requireAuth() default true;
}

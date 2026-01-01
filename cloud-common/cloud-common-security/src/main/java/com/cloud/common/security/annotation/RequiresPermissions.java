package com.cloud.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 用于业务服务的方法级权限控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {
    
    /**
     * 需要的权限列表
     */
    String[] value() default {};
    
    /**
     * 权限验证模式
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑操作枚举
     */
    enum Logical {
        AND, OR
    }
}

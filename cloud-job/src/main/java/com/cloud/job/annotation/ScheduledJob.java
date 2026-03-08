package com.cloud.job.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务注解
 * 标记需要被XXL-JOB调用的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledJob {
    
    /**
     * 任务描述
     */
    String description() default "";
    
    /**
     * Cron表达式
     */
    String cron() default "0 0 2 * * ?";
    
    /**
     * 任务负责人
     */
    String author() default "admin";
    
    /**
     * 报警邮件
     */
    String alarmEmail() default "";
    
    /**
     * 执行器超时时间（秒）
     */
    int timeout() default 3600;
    
    /**
     * 失败重试次数
     */
    int failRetryCount() default 0;
    
    /**
     * 任务分组
     */
    String group() default "DEFAULT";
}

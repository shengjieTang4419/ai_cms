package com.cloud.common.security.config;

import com.cloud.common.security.interceptor.UserContextInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户上下文自动配置
 * 自动注册 UserContextInterceptor 拦截器
 */
@AutoConfiguration
public class UserContextAutoConfiguration {

    @Bean
    public WebMvcConfigurer userContextWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor())
                        .addPathPatterns("/**")
                        .excludePathPatterns("/health", "/actuator/**", "/favicon.ico");
            }
        };
    }
}

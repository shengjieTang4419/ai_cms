package com.cloud.cloud.common.security.adapter;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 用户详情服务适配器
 * 业务模块需要实现这个接口来提供用户认证功能
 */
public interface UserDetailsServiceAdapter extends UserDetailsService {

}

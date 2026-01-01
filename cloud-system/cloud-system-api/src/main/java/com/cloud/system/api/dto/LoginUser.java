package com.cloud.system.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:登录用户信息
 * @date 2025/12/13 18:42
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /**
     * 作为每个登录用户的唯一标识符，用于区分不同的用户会话
     * 连接了JWT token、Redis缓存和用户会话管理
     */
    private String token;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     * Auth模块 要用来进行校验的
     */
    private String password;

    /**
     * 状态
     */
    private Integer status;

    private String ipaddr;

    /**
     * 后续实现
     * 角色列表
     */
    private Set<String> roles;

    /**
     * 后续实现
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;


    public LoginUser(Integer userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
}

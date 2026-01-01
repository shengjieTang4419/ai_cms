package com.cloud.system.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限实体
 */
@Data
@Entity
@Table(name = "sys_permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String permissionCode; // 权限代码，如：USER_CREATE, USER_READ, CHAT_SEND

    @Column(nullable = false)
    private String permissionName; // 权限名称，如：创建用户, 查看用户, 发送消息

    private String description; // 权限描述

    private String resource; // 资源路径，如：/api/users, /api/chat

    private String method; // HTTP方法，如：GET, POST, PUT, DELETE

    private Integer sort; // 排序

    @Column(name = "status")
    private Integer status; // 状态：0-禁用, 1-启用

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Permission parent; // 上级权限，用于构建权限树

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Permission> children = new ArrayList<>();

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private List<Role> roles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 1; // 默认启用
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

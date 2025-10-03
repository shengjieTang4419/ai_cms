package com.cloud.cloud.system.service;

import com.cloud.cloud.system.domain.Permission;
import com.cloud.cloud.system.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 权限服务
 */
@Service
@Transactional
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * 创建权限
     */
    public Permission createPermission(Permission permission) {
        if (permissionRepository.existsByPermissionCode(permission.getPermissionCode())) {
            throw new RuntimeException("权限代码已存在: " + permission.getPermissionCode());
        }
        return permissionRepository.save(permission);
    }

    /**
     * 更新权限
     */
    public Permission updatePermission(Permission permission) {
        Permission existingPermission = permissionRepository.findById(permission.getId())
                .orElseThrow(() -> new RuntimeException("权限不存在: " + permission.getId()));

        // 检查权限代码是否重复（排除自己）
        if (!existingPermission.getPermissionCode().equals(permission.getPermissionCode()) &&
            permissionRepository.existsByPermissionCode(permission.getPermissionCode())) {
            throw new RuntimeException("权限代码已存在: " + permission.getPermissionCode());
        }

        existingPermission.setPermissionName(permission.getPermissionName());
        existingPermission.setDescription(permission.getDescription());
        existingPermission.setResource(permission.getResource());
        existingPermission.setMethod(permission.getMethod());
        existingPermission.setSort(permission.getSort());
        existingPermission.setStatus(permission.getStatus());

        return permissionRepository.save(existingPermission);
    }

    /**
     * 删除权限
     */
    public void deletePermission(Long permissionId) {
        permissionRepository.deleteById(permissionId);
    }

    /**
     * 根据ID查找权限
     */
    @Transactional(readOnly = true)
    public Permission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
    }

    /**
     * 根据权限代码查找权限
     */
    @Transactional(readOnly = true)
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        return permissionRepository.findByPermissionCode(permissionCode);
    }

    /**
     * 根据资源路径和方法查找权限
     */
    @Transactional(readOnly = true)
    public Optional<Permission> findByResourceAndMethod(String resource, String method) {
        return permissionRepository.findByResourceAndMethod(resource, method);
    }

    /**
     * 查找所有启用的权限
     */
    @Transactional(readOnly = true)
    public List<Permission> findAllActivePermissions() {
        return permissionRepository.findByStatus(1);
    }

    /**
     * 查找所有权限
     */
    @Transactional(readOnly = true)
    public List<Permission> findAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 查找顶级权限（权限树）
     */
    @Transactional(readOnly = true)
    public List<Permission> findRootPermissions() {
        return permissionRepository.findRootPermissions();
    }

    /**
     * 根据父权限ID查找子权限
     */
    @Transactional(readOnly = true)
    public List<Permission> findByParentId(Long parentId) {
        return permissionRepository.findByParentId(parentId);
    }

    /**
     * 根据角色ID查找权限
     */
    @Transactional(readOnly = true)
    public List<Permission> findByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    /**
     * 检查权限代码是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByPermissionCode(String permissionCode) {
        return permissionRepository.existsByPermissionCode(permissionCode);
    }
}

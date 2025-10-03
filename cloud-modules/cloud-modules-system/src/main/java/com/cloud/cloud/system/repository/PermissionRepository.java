package com.cloud.cloud.system.repository;

import com.cloud.cloud.system.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限仓库
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限代码查找权限
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * 根据权限名称查找权限
     */
    Optional<Permission> findByPermissionName(String permissionName);

    /**
     * 查找所有启用的权限
     */
    List<Permission> findByStatus(Integer status);

    /**
     * 根据资源路径和方法查找权限
     */
    Optional<Permission> findByResourceAndMethod(String resource, String method);

    /**
     * 检查权限代码是否存在
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * 查找顶级权限（没有上级权限的）
     */
    @Query("SELECT p FROM Permission p WHERE p.parent IS NULL AND p.status = 1 ORDER BY p.sort")
    List<Permission> findRootPermissions();

    /**
     * 根据父权限ID查找子权限
     */
    @Query("SELECT p FROM Permission p WHERE p.parent.id = :parentId AND p.status = 1 ORDER BY p.sort")
    List<Permission> findByParentId(Long parentId);

    /**
     * 根据角色ID查找权限
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.status = 1")
    List<Permission> findByRoleId(Long roleId);
}

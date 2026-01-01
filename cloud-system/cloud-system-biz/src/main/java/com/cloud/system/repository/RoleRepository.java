package com.cloud.system.repository;

import com.cloud.system.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓库
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色代码查找角色
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 根据角色名称查找角色
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 查找所有启用的角色
     */
    List<Role> findByStatus(Integer status);

    /**
     * 检查角色代码是否存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 根据角色代码列表查找角色
     */
    @Query("SELECT r FROM Role r WHERE r.roleCode IN :roleCodes AND r.status = 1")
    List<Role> findByRoleCodesAndStatus(List<String> roleCodes);
}

package com.cloud.system.service;


import com.cloud.system.domain.Role;
import com.cloud.system.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 角色服务
 */
@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        if (roleRepository.existsByRoleCode(role.getRoleCode())) {
            throw new RuntimeException("角色代码已存在: " + role.getRoleCode());
        }
        return roleRepository.save(role);
    }

    /**
     * 更新角色
     */
    public Role updateRole(Role role) {
        Role existingRole = roleRepository.findById(role.getId())
                .orElseThrow(() -> new RuntimeException("角色不存在: " + role.getId()));

        // 检查角色代码是否重复（排除自己）
        if (!existingRole.getRoleCode().equals(role.getRoleCode()) &&
                roleRepository.existsByRoleCode(role.getRoleCode())) {
            throw new RuntimeException("角色代码已存在: " + role.getRoleCode());
        }

        existingRole.setRoleName(role.getRoleName());
        existingRole.setDescription(role.getDescription());
        existingRole.setSort(role.getSort());
        existingRole.setStatus(role.getStatus());

        return roleRepository.save(existingRole);
    }

    /**
     * 删除角色
     */
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
    }

    /**
     * 根据ID查找角色
     */
    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
    }

    /**
     * 根据角色代码查找角色
     */
    @Transactional(readOnly = true)
    public Optional<Role> findByRoleCode(String roleCode) {
        return roleRepository.findByRoleCode(roleCode);
    }

    /**
     * 查找所有启用的角色
     */
    @Transactional(readOnly = true)
    public List<Role> findAllActiveRoles() {
        return roleRepository.findByStatus(1);
    }

    /**
     * 查找所有角色
     */
    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 检查角色代码是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByRoleCode(String roleCode) {
        return roleRepository.existsByRoleCode(roleCode);
    }
}

package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.mapper.SysRolePermissionMapper;
import com.qingmou.cxnp.service.RolePermissionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    public RolePermissionServiceImpl(
            SysRolePermissionMapper sysRolePermissionMapper
    ) {
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Override
    @Transactional
    public void bindPermission(Long roleId, Long permissionId) {

        // 1. 参数校验
        if (roleId == null || permissionId == null
                || roleId <= 0 || permissionId <= 0) {
            throw new IllegalArgumentException("角色ID或权限ID不合法");
        }

        // 2. Service 层检查是否重复绑定
        int count = sysRolePermissionMapper.countByRoleIdAndPermissionId(
                roleId, permissionId
        );

        if (count > 0) {
            throw new IllegalArgumentException("角色已绑定该权限");
        }

        // 3. 插入绑定，数据库唯一约束兜底
        try {
            sysRolePermissionMapper.insert(roleId, permissionId);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("角色已绑定该权限", e);
        }
    }
}
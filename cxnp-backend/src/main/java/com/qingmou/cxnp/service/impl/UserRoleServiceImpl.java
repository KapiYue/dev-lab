package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.mapper.SysUserRoleMapper;
import com.qingmou.cxnp.service.UserRoleService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    public UserRoleServiceImpl(SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    @Transactional
    public void bindRole(Long userId, Long roleId) {

        if (userId == null || roleId == null || userId <= 0 || roleId <= 0) {
            throw new IllegalArgumentException("用户ID或角色ID不合法");
        }

        // 1. Service 层前置校验
        int count = sysUserRoleMapper.countByUserIdAndRoleId(userId, roleId);

        if (count > 0) {
            throw new IllegalArgumentException("用户已绑定该角色");
        }

        // 2. 插入绑定，数据库唯一约束兜底
        try {
            sysUserRoleMapper.insert(userId, roleId);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("用户已绑定该角色", e);
        }
    }
}
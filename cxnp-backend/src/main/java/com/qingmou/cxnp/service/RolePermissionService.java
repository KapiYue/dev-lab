package com.qingmou.cxnp.service;

public interface RolePermissionService {

    // 为指定角色绑定权限
    void bindPermission(Long roleId, Long permissionId);
}
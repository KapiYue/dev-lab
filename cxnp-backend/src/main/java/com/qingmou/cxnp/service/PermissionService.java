package com.qingmou.cxnp.service;

import com.qingmou.cxnp.entity.SysPermission;

import java.util.List;

public interface PermissionService {

    // 根据用户 ID 查询功能权限
    List<SysPermission> getPermissionsByUserId(Long userId);

    // 判断指定用户是否拥有某项功能权限
    boolean hasPermission(Long userId, String permissionKey);

    // 校验指定用户是否拥有权限，无权限时抛出异常
    void checkPermission(Long userId, String permissionKey);

    // 查询当前登录用户拥有的功能权限
    List<SysPermission> getCurrentUserPermissions();

    // 校验当前登录用户是否拥有指定功能权限
    void checkCurrentUserPermission(String permissionKey);
}


package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.entity.SysPermission;
import com.qingmou.cxnp.mapper.SysPermissionMapper;
import com.qingmou.cxnp.service.PermissionService;
import org.springframework.stereotype.Service;
import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.exception.PermissionDeniedException;
import com.qingmou.cxnp.exception.AuthenticationException;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    public PermissionServiceImpl(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<SysPermission> getPermissionsByUserId(Long userId) {
        return sysPermissionMapper.selectByUserId(userId);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        List<SysPermission> permissions = getPermissionsByUserId(userId);

        return permissions.stream()
                .anyMatch(permission ->
                        permissionCode.equals(permission.getPermissionCode())
                );
    }

    @Override
    public void checkPermission(Long userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            throw new PermissionDeniedException(
                    "无功能权限：" + permissionCode
            );
        }
    }

    @Override
    public List<SysPermission> getCurrentUserPermissions() {

        // 获取当前登录用户 ID
        Long userId = UserContext.getUserId();

        // 未登录时禁止继续查询
        if (userId == null) {
            throw new AuthenticationException("用户未登录");
        }

        // 查询当前登录用户拥有的功能权限
        return getPermissionsByUserId(userId);
    }


    @Override
    public void checkCurrentUserPermission(String permissionCode) {

        // 从登录上下文获取当前用户 ID
        Long userId = UserContext.getUserId();

        // 未登录直接拒绝
        if (userId == null) {
            throw new AuthenticationException("用户未登录");
        }

        // 复用已有的权限校验方法
        checkPermission(userId, permissionCode);
    }

}

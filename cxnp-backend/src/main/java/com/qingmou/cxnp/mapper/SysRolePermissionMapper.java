package com.qingmou.cxnp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRolePermissionMapper {

    // 查询角色是否已经绑定指定权限
    int countByRoleIdAndPermissionId(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId
    );

    // 新增角色权限绑定
    int insert(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId
    );
}
package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    // 查询所有功能权限
    List<SysPermission> selectAll();

    // 根据权限编码查询权限
    SysPermission selectByPermissionCode(
            @Param("permissionCode") String permissionCode
    );

    // 根据用户 ID 查询该用户拥有的全部功能权限
    List<SysPermission> selectByUserId(
            @Param("userId") Long userId
    );
}

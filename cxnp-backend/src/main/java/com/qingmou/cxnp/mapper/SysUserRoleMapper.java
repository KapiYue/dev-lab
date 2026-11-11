package com.qingmou.cxnp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserRoleMapper {

    // 检查用户是否已经绑定指定角色
    int countByUserIdAndRoleId(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );

    // 新增用户角色绑定
    int insert(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );
}
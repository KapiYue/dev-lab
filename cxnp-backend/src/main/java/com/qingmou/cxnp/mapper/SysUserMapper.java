package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {

    // 根据用户名查询系统用户
    SysUser selectByUsername(
            @Param("username") String username
    );

    // 根据用户 ID 查询系统用户
    SysUser selectById(
            @Param("id") Long id
    );
}
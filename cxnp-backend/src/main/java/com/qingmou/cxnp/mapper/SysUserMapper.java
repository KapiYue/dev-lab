package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {

    SysUser selectByUsername(
            @Param("username") String username
    );
}
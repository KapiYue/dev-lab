package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    // 查询所有角色
    List<SysRole> selectAll();

    // 根据角色编码查询角色
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);

    // 根据用户 ID 查询该用户拥有的角色
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}

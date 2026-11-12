package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 数据库中的角色数据范围读取接口。 */
@Mapper
public interface RoleDataScopeMapper {

    /**
     * 只查询当前用户拥有、启用且已授予指定功能权限的角色范围。
     * 不能将用户的其他角色范围并入结果。
     */
    List<SysRoleDataScope> selectEffectiveScopesByUserIdAndPermissionKey(
            @Param("userId") Long userId,
            @Param("permissionKey") String permissionKey
    );

    /** 查询角色显式配置的启用组织。 */
    List<Long> selectActiveOrgIdsByRoleId(@Param("roleId") Long roleId);

    /** 查询一个启用组织及其所有启用下级组织。 */
    List<SysOrg> selectActiveOrgAndChildren(@Param("orgId") Long orgId);
}

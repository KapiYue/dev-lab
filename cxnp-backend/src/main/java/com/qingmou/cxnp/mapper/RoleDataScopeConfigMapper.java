package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 角色数据范围配置的写入 Mapper。 */
@Mapper
public interface RoleDataScopeConfigMapper {

    SysRoleDataScope selectScopeByRoleId(@Param("roleId") Long roleId);

    int insertScope(
            @Param("roleId") Long roleId,
            @Param("scopeType") RoleDataScopeType scopeType
    );

    int updateScopeType(
            @Param("roleId") Long roleId,
            @Param("scopeType") RoleDataScopeType scopeType
    );

    int deleteOrgBindingsByRoleId(@Param("roleId") Long roleId);

    int countOrgBinding(
            @Param("roleId") Long roleId,
            @Param("orgId") Long orgId
    );

    int insertOrgBinding(
            @Param("roleId") Long roleId,
            @Param("orgId") Long orgId
    );

    List<Long> selectActiveOrgIdsByIds(@Param("orgIds") List<Long> orgIds);
}

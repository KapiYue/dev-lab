
package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataScopeMapper {

    // 根据用户 ID 查询有效角色的数据范围配置
    List<SysRoleDataScope> selectScopesByUserId(
            @Param("userId") Long userId
    );

    // 查询某个角色绑定的组织 ID
    List<Long> selectOrgIdsByRoleId(
            @Param("roleId") Long roleId
    );

    // 查询当前组织及所有下级组织
    List<SysOrg> selectOrgAndChildren(
            @Param("orgId") Long orgId
    );

    // 根据 ID 查询组织
    SysOrg selectOrgById(
            @Param("orgId") Long orgId
    );
}

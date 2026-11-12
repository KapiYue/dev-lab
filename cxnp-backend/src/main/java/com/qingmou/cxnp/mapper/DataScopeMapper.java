
package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataScopeMapper {

    // 查询当前组织及所有下级组织
    List<SysOrg> selectOrgAndChildren(
            @Param("orgId") Long orgId
    );

    // 根据 ID 查询组织
    SysOrg selectOrgById(
            @Param("orgId") Long orgId
    );
}

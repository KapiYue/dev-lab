package com.qingmou.cxnp.service;

import com.qingmou.cxnp.dto.RoleDataScopeConfigDTO;

/** 角色数据范围及其组织绑定的事务写入服务。 */
public interface RoleDataScopeConfigService {

    /** 覆盖配置一个角色的数据范围及其组织集合。 */
    void configure(RoleDataScopeConfigDTO config);

    /** 为已配置 CUSTOM/ORG_AND_CHILD 范围的角色追加一个组织。 */
    void bindOrg(Long roleId, Long orgId);
}

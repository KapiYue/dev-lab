package com.qingmou.cxnp.service;

import com.qingmou.cxnp.vo.RoleDataScopeResultVO;

/** 计算当前登录用户在指定功能上的角色数据范围。 */
public interface RoleDataScopeService {

    /**
     * 从后端 UserContext 取得当前用户，并计算其可访问的党员数据范围。
     *
     * @throws com.qingmou.cxnp.exception.AuthenticationException 未登录
     * @throws com.qingmou.cxnp.exception.DataScopeDeniedException 没有有效数据范围
     */
    RoleDataScopeResultVO getCurrentUserAccessibleScope(String permissionKey);
}

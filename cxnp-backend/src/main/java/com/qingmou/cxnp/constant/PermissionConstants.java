
package com.qingmou.cxnp.constant;

public final class PermissionConstants {

    // 党员列表查询权限
    public static final String PARTY_MEMBER_LIST =
            "party:member:list";

    // 党员详情查询权限
    public static final String PARTY_MEMBER_DETAIL =
            "party:member:detail";

    // 管理员强制撤销用户全部会话权限
    public static final String RBAC_USER_SESSION_REVOKE =
            "rbac:user:session:revoke";

    // 禁止实例化工具类
    private PermissionConstants() {
    }
}

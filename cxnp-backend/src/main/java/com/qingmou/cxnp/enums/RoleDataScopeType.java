package com.qingmou.cxnp.enums;

/**
 * 角色授权给党员业务的数据范围。
 *
 * <p>该枚举与岗位/项目授权使用的 {@link DataScopeType} 相互独立。尤其是
 * {@link #SELF} 表示当前登录用户本人的党员记录，而不是任职所在组织。</p>
 */
public enum RoleDataScopeType {

    /** 当前角色在对应功能上的全部党员数据。 */
    ALL,

    /** 角色配置的根组织及其所有启用下级组织。 */
    ORG_AND_CHILD,

    /** 角色显式绑定的启用组织。 */
    CUSTOM,

    /** 当前登录用户关联的党员记录。 */
    SELF
}

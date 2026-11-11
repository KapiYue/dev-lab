package com.qingmou.cxnp.enums;

public enum DataScopeType {

    // 全部数据
    ALL,

    // 本组织及下级组织
    ORG_AND_CHILD,

    // 指定组织或党支部
    CUSTOM,

    // 当前任职所属组织（不含下级），不是个人记录级范围
    SELF
}

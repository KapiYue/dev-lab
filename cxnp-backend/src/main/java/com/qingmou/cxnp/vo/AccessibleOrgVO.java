package com.qingmou.cxnp.vo;

// VO是表示可访问组织信息
public record AccessibleOrgVO(
    Long orgId,
    String orgName,
    Long parentId,
    String orgType
) {
}
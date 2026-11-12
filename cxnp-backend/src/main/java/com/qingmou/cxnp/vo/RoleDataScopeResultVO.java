package com.qingmou.cxnp.vo;

import java.util.Set;

/**
 * 当前用户针对一个功能权限计算出的角色数据范围。
 *
 * <p>范围仅来自拥有 {@code permissionKey} 的有效角色。{@code includeSelf}
 * 表示允许本人党员记录；它可以与组织范围同时存在，表示多个角色授权结果的并集。</p>
 */
// record 的访问方式是 user.id()，而不是传统的 user.getId()
public record RoleDataScopeResultVO(
        Long userId,
        String permissionKey,
        boolean all,
        boolean includeSelf,
        Set<Long> orgIds
) {
    public RoleDataScopeResultVO {
        // orgIds 为 null创建一个空的、不可修改的集合
        orgIds = orgIds == null ? Set.of() : Set.copyOf(orgIds);
    }

    public boolean isEmpty() {
        return !all && !includeSelf && orgIds.isEmpty();
    }

    public boolean canAccessOrg(Long orgId) {
        return orgId != null && (all || orgIds.contains(orgId));
    }
}


package com.qingmou.cxnp.vo;

import java.util.Set;

public record DataScopeResultVO(
        Long projectId,
        String moduleCode,
        String permissionCode,
        boolean all,
        // SELF 已被解析为 orgIds 中的当前任职组织；该字段保留兼容，
        // 不表示个人记录级授权，也不能用于绕过 orgIds 的项目边界。
        boolean self,
        Set<Long> orgIds
) {

    public DataScopeResultVO {
        orgIds = orgIds == null
                ? Set.of()
                : Set.copyOf(orgIds);
    }

    // 当前操作没有任何有效的数据范围
    public boolean isEmpty() {
        return !all && orgIds.isEmpty();
    }

     // 当前项目组织级访问检查
    // 包括 ALL 在内，都不能绕过项目组织边界
    public boolean canAccessOrg(Long orgId) {
        return orgId != null && orgIds.contains(orgId);
    }
}

package com.qingmou.cxnp.dto;

import com.qingmou.cxnp.enums.RoleDataScopeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 角色数据范围配置请求。
 *
 * <p>
 * 组织绑定规则由 Service 校验：CUSTOM 与 ORG_AND_CHILD 至少一个组织，
 * ALL 与 SELF 不允许组织绑定。
 * </p>
 */
public record RoleDataScopeConfigDTO(
        @NotNull @Positive Long roleId,
        @NotNull RoleDataScopeType scopeType,
        List<@NotNull @Positive Long> orgIds) {
    public RoleDataScopeConfigDTO {
        orgIds = orgIds == null ? List.of() : List.copyOf(orgIds);
    }
}

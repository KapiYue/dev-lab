package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.exception.AuthenticationException;
import com.qingmou.cxnp.exception.DataScopeDeniedException;
import com.qingmou.cxnp.mapper.RoleDataScopeMapper;
import com.qingmou.cxnp.service.RoleDataScopeService;
import com.qingmou.cxnp.vo.RoleDataScopeResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 角色数据范围计算实现，不参与岗位/项目数据范围计算。 */
@Service
public class RoleDataScopeServiceImpl implements RoleDataScopeService {

    private final RoleDataScopeMapper roleDataScopeMapper;

    public RoleDataScopeServiceImpl(RoleDataScopeMapper roleDataScopeMapper) {
        this.roleDataScopeMapper = roleDataScopeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDataScopeResultVO getCurrentUserAccessibleScope(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            throw new IllegalArgumentException("功能权限标识不能为空");
        }

        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new AuthenticationException("未登录或登录已过期");
        }

        List<SysRoleDataScope> scopes = roleDataScopeMapper
                .selectEffectiveScopesByUserIdAndPermissionKey(
                        currentUserId,
                        permissionKey
                );

        boolean includeSelf = false;
        Set<Long> accessibleOrgIds = new HashSet<>();

        if (scopes != null) {
            for (SysRoleDataScope scope : scopes) {
                if (scope == null || scope.getScopeType() == null) {
                    continue;
                }

                RoleDataScopeType scopeType = scope.getScopeType();
                if (scopeType == RoleDataScopeType.ALL) {
                    return new RoleDataScopeResultVO(
                            currentUserId,
                            permissionKey,
                            true,
                            false,
                            Set.of()
                    );
                }

                if (scopeType == RoleDataScopeType.SELF) {
                    includeSelf = true;
                    continue;
                }

                if (scope.getRoleId() == null) {
                    continue;
                }

                List<Long> configuredOrgIds = roleDataScopeMapper
                        .selectActiveOrgIdsByRoleId(scope.getRoleId());

                if (scopeType == RoleDataScopeType.CUSTOM) {
                    addOrgIds(accessibleOrgIds, configuredOrgIds);
                } else if (scopeType == RoleDataScopeType.ORG_AND_CHILD) {
                    addOrgAndChildren(accessibleOrgIds, configuredOrgIds);
                }
            }
        }

        RoleDataScopeResultVO result = new RoleDataScopeResultVO(
                currentUserId,
                permissionKey,
                false,
                includeSelf,
                accessibleOrgIds
        );

        if (result.isEmpty()) {
            throw new DataScopeDeniedException("当前用户没有有效数据范围");
        }

        return result;
    }

    private void addOrgAndChildren(Set<Long> targetOrgIds, List<Long> rootOrgIds) {
        if (rootOrgIds == null) {
            return;
        }

        for (Long rootOrgId : rootOrgIds) {
            if (rootOrgId == null) {
                continue;
            }

            List<SysOrg> orgs = roleDataScopeMapper
                    .selectActiveOrgAndChildren(rootOrgId);
            if (orgs == null) {
                continue;
            }

            for (SysOrg org : orgs) {
                if (org != null && org.getId() != null) {
                    targetOrgIds.add(org.getId());
                }
            }
        }
    }

    private void addOrgIds(Set<Long> targetOrgIds, List<Long> orgIds) {
        if (orgIds == null) {
            return;
        }

        for (Long orgId : orgIds) {
            if (orgId != null) {
                targetOrgIds.add(orgId);
            }
        }
    }
}

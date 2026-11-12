package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.dto.RoleDataScopeConfigDTO;
import com.qingmou.cxnp.entity.SysRole;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.mapper.RoleDataScopeConfigMapper;
import com.qingmou.cxnp.mapper.SysRoleMapper;
import com.qingmou.cxnp.service.RoleDataScopeConfigService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleDataScopeConfigServiceImpl implements RoleDataScopeConfigService {

    private final RoleDataScopeConfigMapper roleDataScopeConfigMapper;
    private final SysRoleMapper sysRoleMapper;

    public RoleDataScopeConfigServiceImpl(
            RoleDataScopeConfigMapper roleDataScopeConfigMapper,
            SysRoleMapper sysRoleMapper
    ) {
        this.roleDataScopeConfigMapper = roleDataScopeConfigMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    @Transactional(timeout = 10)
    public void configure(RoleDataScopeConfigDTO config) {
        if (config == null || config.roleId() == null || config.roleId() <= 0
                || config.scopeType() == null) {
            throw new IllegalArgumentException("角色数据范围配置不合法");
        }

        validateActiveRole(config.roleId());
        List<Long> orgIds = validateAndNormalizeOrgIds(config);

        SysRoleDataScope existing = roleDataScopeConfigMapper
                .selectScopeByRoleId(config.roleId());
        if (existing == null) {
            try {
                roleDataScopeConfigMapper.insertScope(config.roleId(), config.scopeType());
            } catch (DuplicateKeyException ex) {
                // 并发首次配置时，唯一约束兜底；随后按覆盖配置语义更新即可。
                roleDataScopeConfigMapper.updateScopeType(
                        config.roleId(), config.scopeType());
            }
        } else {
            roleDataScopeConfigMapper.updateScopeType(config.roleId(), config.scopeType());
        }

        // 先移除旧集合，再写入已完全校验的新集合；任一插入失败将整体回滚。
        roleDataScopeConfigMapper.deleteOrgBindingsByRoleId(config.roleId());
        for (Long orgId : orgIds) {
            insertOrgBinding(config.roleId(), orgId);
        }
    }

    @Override
    @Transactional
    public void bindOrg(Long roleId, Long orgId) {
        if (roleId == null || roleId <= 0 || orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("角色 ID 或组织 ID 不合法");
        }

        validateActiveRole(roleId);
        SysRoleDataScope scope = roleDataScopeConfigMapper.selectScopeByRoleId(roleId);
        if (scope == null || scope.getScopeType() == null) {
            throw new IllegalArgumentException("角色尚未配置数据范围");
        }
        if (scope.getScopeType() != RoleDataScopeType.CUSTOM
                && scope.getScopeType() != RoleDataScopeType.ORG_AND_CHILD) {
            throw new IllegalArgumentException("当前数据范围不允许绑定组织");
        }

        validateActiveOrgIds(List.of(orgId));

        if (roleDataScopeConfigMapper.countOrgBinding(roleId, orgId) > 0) {
            throw new IllegalArgumentException("角色已绑定该组织");
        }

        insertOrgBinding(roleId, orgId);
    }

    private List<Long> validateAndNormalizeOrgIds(RoleDataScopeConfigDTO config) {
        List<Long> sourceOrgIds = config.orgIds();
        for (Long orgId : sourceOrgIds) {
            if (orgId == null || orgId <= 0) {
                throw new IllegalArgumentException("组织 ID 不合法");
            }
        }

        Set<Long> distinctOrgIds = new HashSet<>(sourceOrgIds);
        if (distinctOrgIds.size() != sourceOrgIds.size()) {
            throw new IllegalArgumentException("指定组织存在重复绑定");
        }

        boolean requiresOrg = config.scopeType() == RoleDataScopeType.CUSTOM
                || config.scopeType() == RoleDataScopeType.ORG_AND_CHILD;
        if (requiresOrg && distinctOrgIds.isEmpty()) {
            throw new IllegalArgumentException("当前数据范围必须绑定组织");
        }
        if (!requiresOrg && !distinctOrgIds.isEmpty()) {
            throw new IllegalArgumentException("当前数据范围不允许绑定组织");
        }

        List<Long> normalizedOrgIds = new ArrayList<>(distinctOrgIds);
        validateActiveOrgIds(normalizedOrgIds);
        return normalizedOrgIds;
    }

    private void validateActiveRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!Integer.valueOf(1).equals(role.getStatus())) {
            throw new IllegalArgumentException("角色未启用");
        }
    }

    private void validateActiveOrgIds(List<Long> orgIds) {
        if (orgIds.isEmpty()) {
            return;
        }

        List<Long> activeOrgIds = roleDataScopeConfigMapper
                .selectActiveOrgIdsByIds(orgIds);
        if (activeOrgIds == null || activeOrgIds.size() != orgIds.size()
                || !new HashSet<>(activeOrgIds).containsAll(orgIds)) {
            throw new IllegalArgumentException("存在不存在或未启用的组织");
        }
    }

    private void insertOrgBinding(Long roleId, Long orgId) {
        try {
            roleDataScopeConfigMapper.insertOrgBinding(roleId, orgId);
        } catch (DuplicateKeyException ex) {
            // 并发写入时由 uk_role_org 兜底，仍返回与前置校验一致的业务错误。
            throw new IllegalArgumentException("角色已绑定该组织", ex);
        }
    }
}

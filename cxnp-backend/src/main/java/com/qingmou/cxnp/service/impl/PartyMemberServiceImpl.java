
package com.qingmou.cxnp.service.impl;


import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.mapper.PartyMemberMapper;
import com.qingmou.cxnp.service.PartyMemberService;
import com.qingmou.cxnp.service.PermissionService;
import com.qingmou.cxnp.service.RoleDataScopeService;
import org.springframework.stereotype.Service;
import com.qingmou.cxnp.constant.PermissionConstants;
import com.qingmou.cxnp.vo.RoleDataScopeResultVO;

import java.util.List;

@Service
public class PartyMemberServiceImpl implements PartyMemberService {

    private final PartyMemberMapper partyMemberMapper;
    private final PermissionService permissionService;
    private final RoleDataScopeService roleDataScopeService;

    public PartyMemberServiceImpl(
            PartyMemberMapper partyMemberMapper,
            PermissionService permissionService,
            RoleDataScopeService roleDataScopeService
    ) {
        this.partyMemberMapper = partyMemberMapper;
        this.permissionService = permissionService;
        this.roleDataScopeService = roleDataScopeService;
    }

    @Override
    public List<PartyMember> getAllPartyMembers() {

        // 校验党员列表权限
        permissionService.checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_LIST
        );

        RoleDataScopeResultVO dataScope = roleDataScopeService
                .getCurrentUserAccessibleScope(
                        PermissionConstants.PARTY_MEMBER_LIST
                );

        return partyMemberMapper.selectByDataScope(
                dataScope.all(),
                dataScope.includeSelf(),
                dataScope.userId(),
                dataScope.orgIds()
        );
    }

    @Override
    public PartyMember getPartyMemberById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("党员 ID 必须大于 0");
        }

        // 校验党员详情权限
        permissionService.checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_DETAIL
        );

        RoleDataScopeResultVO dataScope = roleDataScopeService
                .getCurrentUserAccessibleScope(
                        PermissionConstants.PARTY_MEMBER_DETAIL
                );

        // 不先查全量详情再在内存判断；SQL 同时限制 ID 和授权范围。
        // 未找到与越权均返回 null，由 Controller 统一按不可见资源处理。
        return partyMemberMapper.selectByIdWithinDataScope(
                id,
                dataScope.all(),
                dataScope.includeSelf(),
                dataScope.userId(),
                dataScope.orgIds()
        );
    }
}

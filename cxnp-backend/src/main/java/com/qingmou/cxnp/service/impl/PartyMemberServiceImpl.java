
package com.qingmou.cxnp.service.impl;


import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.mapper.PartyMemberMapper;
import com.qingmou.cxnp.service.PartyMemberService;
import com.qingmou.cxnp.service.PermissionService;
import org.springframework.stereotype.Service;
import com.qingmou.cxnp.constant.PermissionConstants;

import java.util.List;

@Service
public class PartyMemberServiceImpl implements PartyMemberService {

    private final PartyMemberMapper partyMemberMapper;
    private final PermissionService permissionService;

    public PartyMemberServiceImpl(
            PartyMemberMapper partyMemberMapper,
            PermissionService permissionService
    ) {
        this.partyMemberMapper = partyMemberMapper;
        this.permissionService = permissionService;
    }

    @Override
    public List<PartyMember> getAllPartyMembers() {

        // 校验党员列表权限
        permissionService.checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_LIST
        );

        return partyMemberMapper.selectAll();
    }

    @Override
    public PartyMember getPartyMemberById(Long id) {

        // 校验党员详情权限
        permissionService.checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_DETAIL
        );

        return partyMemberMapper.selectById(id);
    }
}

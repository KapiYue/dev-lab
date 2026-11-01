package com.qingmou.cxnp.service;

import com.qingmou.cxnp.entity.PartyMember;
import java.util.List;

public interface PartyMemberService {

    // 查询所有党员
    List<PartyMember> getAllPartyMembers();

    // 根据 ID 查询党员详情
    PartyMember getPartyMemberById(Long id);

}

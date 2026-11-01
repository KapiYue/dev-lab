package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.mapper.PartyMemberMapper;
import com.qingmou.cxnp.service.PartyMemberService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PartyMemberServiceImpl implements PartyMemberService {

    private final PartyMemberMapper partyMemberMapper;

    public PartyMemberServiceImpl(PartyMemberMapper partyMemberMapper){
        this.partyMemberMapper = partyMemberMapper;
    }

    // Override 实现接口定义的方法，也是java自带的
    @Override
    public List<PartyMember> getAllPartyMembers(){
        return partyMemberMapper.selectAll();
    }

    @Override
    public PartyMember getPartyMemberById(Long id) {
        return partyMemberMapper.selectById(id);
    }
}

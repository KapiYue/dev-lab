package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.service.PartyMemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/party-members")
public class PartyMemberController {

  private final PartyMemberService partyMemberService;

  public PartyMemberController(PartyMemberService partyMemberService){
      this.partyMemberService = partyMemberService;
  }

  @GetMapping
  public Result<List<PartyMember>> list(){
      return Result.success(partyMemberService.getAllPartyMembers());
  }
}

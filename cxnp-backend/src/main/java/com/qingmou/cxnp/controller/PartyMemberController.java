package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.service.PartyMemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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

//  @GetMapping("/{id}")
//    public Result<PartyMember> detail(@PathVariable Long id){
//      PartyMember member = partyMemberService.getPartyMemberById(id);
//      return Result.success(member);
//  }
    @GetMapping("/{id}")
    public ResponseEntity<Result<PartyMember>> detail(@PathVariable Long id){
// 2.增加ID校验
        if (id <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(Result.error(400, "ID 必须大于 0"));
        }
//      1.增加了返回统一JSON格式
      PartyMember member = partyMemberService.getPartyMemberById(id);
      if(member == null){
          return ResponseEntity
                  .status(HttpStatus.NOT_FOUND)
                  .body(Result.error(404, "党员不存在"));
      }
      return ResponseEntity.ok(Result.success(member));
    }

}

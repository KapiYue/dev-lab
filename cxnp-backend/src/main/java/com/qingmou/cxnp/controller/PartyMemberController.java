package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.annotation.RequirePermission;
import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.constant.PermissionConstants;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.service.PartyMemberService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/party-members")
public class PartyMemberController {

    private final PartyMemberService partyMemberService;

    public PartyMemberController(
            PartyMemberService partyMemberService
    ) {
        this.partyMemberService = partyMemberService;
    }

    // 查询党员列表
    @RequirePermission(PermissionConstants.PARTY_MEMBER_LIST)
    @GetMapping
    public Result<List<PartyMember>> list() {

        // Service 内部仍然会进行二次权限校验
        return Result.success(
                partyMemberService.getAllPartyMembers()
        );
    }

    // 根据 ID 查询党员详情
    @RequirePermission(PermissionConstants.PARTY_MEMBER_DETAIL)
    @GetMapping("/{id}")
    public ResponseEntity<Result<PartyMember>> detail(
            @PathVariable Long id
    ) {

        // 参数校验
        if (id <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(Result.error(400, "ID 必须大于 0"));
        }

        // Service 内部进行二次权限校验
        PartyMember member =
                partyMemberService.getPartyMemberById(id);

        // 党员不存在
        if (member == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Result.error(404, "党员不存在"));
        }

        // 查询成功
        return ResponseEntity.ok(
                Result.success(member)
        );
    }
}

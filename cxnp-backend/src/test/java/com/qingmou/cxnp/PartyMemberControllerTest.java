
package com.qingmou.cxnp;

import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.controller.PartyMemberController;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.service.PartyMemberService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyMemberControllerTest {

    private PartyMemberService partyMemberService;

    private PartyMemberController controller;

    @BeforeEach
    void setUp() {

        partyMemberService = mock(PartyMemberService.class);

        controller = new PartyMemberController(
                partyMemberService
        );
    }

    // 1. 查询党员列表成功
    @Test
    void testListSuccess() {

        PartyMember member = new PartyMember();
        member.setId(1L);

        when(partyMemberService.getAllPartyMembers())
                .thenReturn(List.of(member));

        Result<List<PartyMember>> result = controller.list();

        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());

        verify(partyMemberService).getAllPartyMembers();
    }

    // 2. 查询党员空列表
    @Test
    void testListEmpty() {

        when(partyMemberService.getAllPartyMembers())
                .thenReturn(List.of());

        Result<List<PartyMember>> result = controller.list();

        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());

        verify(partyMemberService).getAllPartyMembers();
    }

    // 3. 查询党员详情成功
    @Test
    void testDetailSuccess() {

        Long memberId = 1L;

        PartyMember member = new PartyMember();
        member.setId(memberId);

        when(partyMemberService.getPartyMemberById(memberId))
                .thenReturn(member);

        ResponseEntity<Result<PartyMember>> response =
                controller.detail(memberId);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());

        assertEquals(
                memberId,
                response.getBody().getData().getId()
        );

        verify(partyMemberService)
                .getPartyMemberById(memberId);
    }

    // 4. 非法 ID 返回 400
    @Test
    void testDetailInvalidId() {

        ResponseEntity<Result<PartyMember>> response =
                controller.detail(0L);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        // 参数不合法，不应该调用 Service
        verifyNoInteractions(partyMemberService);
    }

    // 5. 党员不存在返回 404
    @Test
    void testDetailNotFound() {

        Long memberId = 99999L;

        when(partyMemberService.getPartyMemberById(memberId))
                .thenReturn(null);

        ResponseEntity<Result<PartyMember>> response =
                controller.detail(memberId);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        verify(partyMemberService)
                .getPartyMemberById(memberId);
    }
}

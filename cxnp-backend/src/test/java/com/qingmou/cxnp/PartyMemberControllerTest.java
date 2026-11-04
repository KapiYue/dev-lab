
package com.qingmou.cxnp;

import com.qingmou.cxnp.constant.PermissionConstants;
import com.qingmou.cxnp.controller.PartyMemberController;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.service.PartyMemberService;
import com.qingmou.cxnp.service.PermissionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyMemberControllerTest {

    private PartyMemberService partyMemberService;
    private PermissionService permissionService;
    private PartyMemberController controller;

    @BeforeEach
    void setUp() {
        partyMemberService = mock(PartyMemberService.class);
        permissionService = mock(PermissionService.class);

        controller = new PartyMemberController(
                partyMemberService,
                permissionService
        );
    }

    // 1. 列表接口：有权限
    @Test
    void testListWithPermission() {

        controller.list();

        verify(permissionService).checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_LIST
        );

        verify(partyMemberService).getAllPartyMembers();

        // 验证权限校验先于业务查询
        var order = inOrder(permissionService, partyMemberService);

        order.verify(permissionService).checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_LIST
        );
        order.verify(partyMemberService).getAllPartyMembers();
    }

    // 2. 列表接口：无权限
    @Test
    void testListWithoutPermission() {

        doThrow(new SecurityException("无功能权限"))
                .when(permissionService)
                .checkCurrentUserPermission(
                        PermissionConstants.PARTY_MEMBER_LIST
                );

        assertThrows(
                SecurityException.class,
                () -> controller.list()
        );

        // 校验失败后，不能进入业务 Service
        verifyNoInteractions(partyMemberService);
    }

    // 3. 详情接口：有权限
    @Test
    void testDetailWithPermission() {

        Long id = 1L;

        PartyMember member = new PartyMember();
        member.setId(id);

        when(partyMemberService.getPartyMemberById(id))
                .thenReturn(member);

        ResponseEntity<?> response = controller.detail(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(permissionService).checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_DETAIL
        );

        verify(partyMemberService).getPartyMemberById(id);

        // 详情接口不应该额外检查列表权限
        verify(permissionService, never())
                .checkCurrentUserPermission(
                        PermissionConstants.PARTY_MEMBER_LIST
                );

        var order = inOrder(permissionService, partyMemberService);

        order.verify(permissionService).checkCurrentUserPermission(
                PermissionConstants.PARTY_MEMBER_DETAIL
        );
        order.verify(partyMemberService).getPartyMemberById(id);
    }

    // 4. 详情接口：无权限
    @Test
    void testDetailWithoutPermission() {

        doThrow(new SecurityException("无功能权限"))
                .when(permissionService)
                .checkCurrentUserPermission(
                        PermissionConstants.PARTY_MEMBER_DETAIL
                );

        assertThrows(
                SecurityException.class,
                () -> controller.detail(1L)
        );

        // 权限校验失败，不允许调用业务 Service
        verifyNoInteractions(partyMemberService);
    }
}

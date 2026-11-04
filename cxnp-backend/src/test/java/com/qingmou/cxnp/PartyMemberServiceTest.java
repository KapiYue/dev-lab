
package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.mapper.PartyMemberMapper;
import com.qingmou.cxnp.service.PermissionService;
import com.qingmou.cxnp.service.impl.PartyMemberServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyMemberServiceTest {

    @Mock
    private PartyMemberMapper partyMemberMapper;

    @Mock
    private PermissionService permissionService;

    private PartyMemberServiceImpl partyMemberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        partyMemberService = new PartyMemberServiceImpl(
                partyMemberMapper,
                permissionService
        );
    }

    // 1. 有列表权限：允许查询
    @Test
    void testListWithPermission() {

        when(partyMemberMapper.selectAll())
                .thenReturn(List.of(new PartyMember()));

        List<PartyMember> result =
                partyMemberService.getAllPartyMembers();

        assertEquals(1, result.size());

        // 验证权限校验已经执行
        verify(permissionService).checkCurrentUserPermission(
                "party:member:list"
        );

        // 验证数据库查询已经执行
        verify(partyMemberMapper).selectAll();
    }

    // 2. 无列表权限：禁止查询
    @Test
    void testListWithoutPermission() {

        doThrow(new SecurityException(
                "无功能权限：party:member:list"
        ))
                .when(permissionService)
                .checkCurrentUserPermission("party:member:list");

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> partyMemberService.getAllPartyMembers()
        );

        assertEquals(
                "无功能权限：party:member:list",
                exception.getMessage()
        );

        // 验证已检查权限
        verify(permissionService).checkCurrentUserPermission(
                "party:member:list"
        );

        // 核心：无权限时绝对不能执行党员数据查询
        verifyNoInteractions(partyMemberMapper);
    }

    // 3. 有详情权限：允许查询
    @Test
    void testDetailWithPermission() {

        Long memberId = 1L;

        PartyMember member = new PartyMember();
        member.setId(memberId);

        when(partyMemberMapper.selectById(memberId))
                .thenReturn(member);

        PartyMember result =
                partyMemberService.getPartyMemberById(memberId);

        assertNotNull(result);
        assertEquals(memberId, result.getId());

        // 确认执行详情权限校验
        verify(permissionService).checkCurrentUserPermission(
                "party:member:detail"
        );

        // 确认没有额外检查列表权限
        verify(permissionService, never()).checkCurrentUserPermission(
                "party:member:list"
        );

        // 确认执行详情查询
        verify(partyMemberMapper).selectById(memberId);
    }

    // 4. 无详情权限：禁止查询
    @Test
    void testDetailWithoutPermission() {

        Long memberId = 1L;

        doThrow(new SecurityException(
                "无功能权限：party:member:detail"
        ))
                .when(permissionService)
                .checkCurrentUserPermission("party:member:detail");

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> partyMemberService.getPartyMemberById(memberId)
        );

        assertEquals(
                "无功能权限：party:member:detail",
                exception.getMessage()
        );

        // 确认已执行详情权限校验
        verify(permissionService).checkCurrentUserPermission(
                "party:member:detail"
        );

        // 核心验证：无权限时不允许查询党员数据
        verifyNoInteractions(partyMemberMapper);
    }



}

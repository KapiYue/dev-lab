package com.qingmou.cxnp;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.exception.DataScopeDeniedException;
import com.qingmou.cxnp.mapper.RoleDataScopeMapper;
import com.qingmou.cxnp.service.impl.RoleDataScopeServiceImpl;
import com.qingmou.cxnp.vo.RoleDataScopeResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleDataScopeServiceTest {

    private static final String PARTY_MEMBER_LIST = "party:member:list";

    @Mock
    private RoleDataScopeMapper roleDataScopeMapper;

    private RoleDataScopeServiceImpl roleDataScopeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roleDataScopeService = new RoleDataScopeServiceImpl(roleDataScopeMapper);
        UserContext.setUserId(10L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void mergesCustomOrgAndChildAndSelfScopesOfRolesWithTheSamePermission() {
        when(roleDataScopeMapper.selectEffectiveScopesByUserIdAndPermissionKey(
                10L, PARTY_MEMBER_LIST))
                .thenReturn(List.of(
                        scope(1L, RoleDataScopeType.CUSTOM),
                        scope(2L, RoleDataScopeType.ORG_AND_CHILD),
                        scope(3L, RoleDataScopeType.SELF)
                ));
        when(roleDataScopeMapper.selectActiveOrgIdsByRoleId(1L))
                .thenReturn(List.of(20L));
        when(roleDataScopeMapper.selectActiveOrgIdsByRoleId(2L))
                .thenReturn(List.of(30L));
        when(roleDataScopeMapper.selectActiveOrgAndChildren(30L))
                .thenReturn(List.of(org(30L), org(31L)));

        RoleDataScopeResultVO result = roleDataScopeService
                .getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertFalse(result.all());
        assertTrue(result.includeSelf());
        assertEquals(List.of(20L, 30L, 31L), result.orgIds().stream().sorted().toList());
        verify(roleDataScopeMapper).selectEffectiveScopesByUserIdAndPermissionKey(
                10L, PARTY_MEMBER_LIST);
    }

    @Test
    void allScopeGrantsAllDataForTheCurrentPermission() {
        when(roleDataScopeMapper.selectEffectiveScopesByUserIdAndPermissionKey(
                10L, PARTY_MEMBER_LIST))
                .thenReturn(List.of(scope(1L, RoleDataScopeType.ALL)));

        RoleDataScopeResultVO result = roleDataScopeService
                .getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertTrue(result.all());
        assertTrue(result.canAccessOrg(99L));
    }

    @Test
    void noEffectiveScopeFailsClosed() {
        when(roleDataScopeMapper.selectEffectiveScopesByUserIdAndPermissionKey(
                10L, PARTY_MEMBER_LIST))
                .thenReturn(List.of(scope(1L, RoleDataScopeType.CUSTOM)));
        when(roleDataScopeMapper.selectActiveOrgIdsByRoleId(1L)).thenReturn(List.of());

        DataScopeDeniedException exception = assertThrows(
                DataScopeDeniedException.class,
                () -> roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST)
        );

        assertEquals("当前用户没有有效数据范围", exception.getMessage());
    }

    private SysRoleDataScope scope(Long roleId, RoleDataScopeType scopeType) {
        SysRoleDataScope scope = new SysRoleDataScope();
        scope.setRoleId(roleId);
        scope.setScopeType(scopeType);
        return scope;
    }

    private SysOrg org(Long id) {
        SysOrg org = new SysOrg();
        org.setId(id);
        return org;
    }
}

package com.qingmou.cxnp;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.SysPostDataScope;
import com.qingmou.cxnp.enums.DataScopeType;
import com.qingmou.cxnp.exception.PermissionDeniedException;
import com.qingmou.cxnp.mapper.DataScopeMapper;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import com.qingmou.cxnp.service.TargetPostValidationService;
import com.qingmou.cxnp.service.impl.PostPermissionDelegationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PostPermissionDelegationServiceTest {

    @Mock
    private PostAuthorizationMapper postAuthorizationMapper;
    @Mock
    private TargetPostValidationService targetPostValidationService;
    @Mock
    private ProjectAuthorizationService projectAuthorizationService;
    @Mock
    private DataScopeMapper dataScopeMapper;

    private PostPermissionDelegationServiceImpl delegationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegationService = new PostPermissionDelegationServiceImpl(
                postAuthorizationMapper, targetPostValidationService,
                projectAuthorizationService, dataScopeMapper);
        UserContext.setUserId(10L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void delegatesOnlyWhenTargetOrgIsInSourceScope() {
        SysPostDataScope allScope = new SysPostDataScope();
        allScope.setScopeType(DataScopeType.ALL);
        when(postAuthorizationMapper.selectDelegablePostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L));
        when(postAuthorizationMapper.selectValidModulePermissionId(
                "CONSTRUCTION", "construction:progress:view")).thenReturn(8L);
        when(postAuthorizationMapper.selectActiveOrgIdsByProjectId(1L))
                .thenReturn(List.of(20L));
        when(postAuthorizationMapper.selectScopeByPostId(100L)).thenReturn(allScope);
        when(postAuthorizationMapper.selectOrgIdByPostId(200L)).thenReturn(20L);
        when(postAuthorizationMapper.countPostPermissionGrant(200L, 8L)).thenReturn(0);
        when(postAuthorizationMapper.insertPostPermissionGrant(200L, 8L)).thenReturn(1);
        when(postAuthorizationMapper.insertPermissionDelegation(100L, 200L, 8L)).thenReturn(1);

        assertDoesNotThrow(() -> delegationService.delegatePermission(
                100L, 200L, 1L, "CONSTRUCTION", "construction:progress:view"));

        verify(projectAuthorizationService).checkProjectAccess(1L);
        verify(postAuthorizationMapper).insertPostPermissionGrant(200L, 8L);
        verify(postAuthorizationMapper).insertPermissionDelegation(100L, 200L, 8L);
    }

    @Test
    void rejectsDelegationOutsideSourceScope() {
        SysPostDataScope selfScope = new SysPostDataScope();
        selfScope.setScopeType(DataScopeType.SELF);
        when(postAuthorizationMapper.selectDelegablePostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L));
        when(postAuthorizationMapper.selectValidModulePermissionId(
                "CONSTRUCTION", "construction:progress:view")).thenReturn(8L);
        when(postAuthorizationMapper.selectActiveOrgIdsByProjectId(1L))
                .thenReturn(List.of(20L, 30L));
        when(postAuthorizationMapper.selectScopeByPostId(100L)).thenReturn(selfScope);
        when(postAuthorizationMapper.selectOrgIdByPostId(100L)).thenReturn(20L);
        when(postAuthorizationMapper.selectOrgIdByPostId(200L)).thenReturn(30L);

        assertThrows(PermissionDeniedException.class, () -> delegationService.delegatePermission(
                100L, 200L, 1L, "CONSTRUCTION", "construction:progress:view"));

        verify(postAuthorizationMapper, never()).insertPostPermissionGrant(anyLong(), anyLong());
        verify(postAuthorizationMapper, never()).insertPermissionDelegation(anyLong(), anyLong(), anyLong());
    }

    @Test
    void revokesOnlyAnActiveDelegationCreatedBySourcePost() {
        when(postAuthorizationMapper.selectDelegablePostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L));
        when(postAuthorizationMapper.selectValidModulePermissionId(
                "CONSTRUCTION", "construction:progress:view")).thenReturn(8L);
        when(postAuthorizationMapper.revokeActivePermissionDelegation(100L, 200L, 8L))
                .thenReturn(1);
        when(postAuthorizationMapper.deletePostPermissionGrant(200L, 8L)).thenReturn(1);

        delegationService.revokePermission(
                100L, 200L, 1L, "CONSTRUCTION", "construction:progress:view");

        verify(postAuthorizationMapper).revokeActivePermissionDelegation(100L, 200L, 8L);
        verify(postAuthorizationMapper).deletePostPermissionGrant(200L, 8L);
    }
}

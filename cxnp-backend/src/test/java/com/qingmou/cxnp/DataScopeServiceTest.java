package com.qingmou.cxnp;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.SysPostDataScope;
import com.qingmou.cxnp.enums.DataScopeType;
import com.qingmou.cxnp.mapper.DataScopeMapper;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import com.qingmou.cxnp.service.impl.DataScopeServiceImpl;
import com.qingmou.cxnp.vo.DataScopeResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopeServiceTest {

    @Mock
    private PostAuthorizationMapper postAuthorizationMapper;
    @Mock
    private DataScopeMapper dataScopeMapper;
    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    private DataScopeServiceImpl dataScopeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataScopeService = new DataScopeServiceImpl(
                postAuthorizationMapper, dataScopeMapper, projectAuthorizationService);
        UserContext.setUserId(10L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void allScopeIncludesOnlyActiveProjectOrganizations() {
        when(postAuthorizationMapper.selectActiveOrgIdsByProjectId(1L))
                .thenReturn(List.of(20L, 30L));
        when(postAuthorizationMapper.selectAuthorizedPostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L));
        when(postAuthorizationMapper.selectScopeByPostId(100L)).thenReturn(scope(DataScopeType.ALL));

        DataScopeResultVO result = dataScopeService.getAccessibleScope(
                1L, "CONSTRUCTION", "construction:progress:view");

        assertTrue(result.all());
        assertEquals(List.of(20L, 30L), result.orgIds().stream().sorted().toList());
        assertTrue(result.canAccessOrg(20L));
        verify(projectAuthorizationService).checkProjectAccess(1L);
    }

    @Test
    void selfScopesFromMultiplePostsAreMergedAndRemainOrganizationScoped() {
        when(postAuthorizationMapper.selectActiveOrgIdsByProjectId(1L))
                .thenReturn(List.of(20L, 30L));
        when(postAuthorizationMapper.selectAuthorizedPostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L, 101L));
        when(postAuthorizationMapper.selectScopeByPostId(100L)).thenReturn(scope(DataScopeType.SELF));
        when(postAuthorizationMapper.selectScopeByPostId(101L)).thenReturn(scope(DataScopeType.SELF));
        when(postAuthorizationMapper.selectOrgIdByPostId(100L)).thenReturn(20L);
        when(postAuthorizationMapper.selectOrgIdByPostId(101L)).thenReturn(30L);

        DataScopeResultVO result = dataScopeService.getAccessibleScope(
                1L, "CONSTRUCTION", "construction:progress:view");

        assertFalse(result.all());
        assertFalse(result.self());
        assertEquals(List.of(20L, 30L), result.orgIds().stream().sorted().toList());
    }

    @Test
    void missingPostScopeFailsClosed() {
        when(postAuthorizationMapper.selectActiveOrgIdsByProjectId(1L)).thenReturn(List.of(20L));
        when(postAuthorizationMapper.selectAuthorizedPostIds(
                10L, 1L, "CONSTRUCTION", "construction:progress:view"))
                .thenReturn(List.of(100L));

        DataScopeResultVO result = dataScopeService.getAccessibleScope(
                1L, "CONSTRUCTION", "construction:progress:view");

        assertTrue(result.isEmpty());
        assertFalse(result.canAccessOrg(20L));
    }

    private SysPostDataScope scope(DataScopeType type) {
        SysPostDataScope scope = new SysPostDataScope();
        scope.setScopeType(type);
        return scope;
    }
}

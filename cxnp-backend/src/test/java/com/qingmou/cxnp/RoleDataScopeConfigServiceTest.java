package com.qingmou.cxnp;

import com.qingmou.cxnp.dto.RoleDataScopeConfigDTO;
import com.qingmou.cxnp.entity.SysRole;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.mapper.RoleDataScopeConfigMapper;
import com.qingmou.cxnp.mapper.SysRoleMapper;
import com.qingmou.cxnp.service.impl.RoleDataScopeConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleDataScopeConfigServiceTest {

    @Mock
    private RoleDataScopeConfigMapper roleDataScopeConfigMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    private RoleDataScopeConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RoleDataScopeConfigServiceImpl(
                roleDataScopeConfigMapper,
                sysRoleMapper
        );
        when(sysRoleMapper.selectById(1L)).thenReturn(activeRole(1L));
    }

    @Test
    void configuresCustomScopeAndReplacesItsOrganizationSetInOneServiceCall() {
        when(roleDataScopeConfigMapper.selectActiveOrgIdsByIds(anyList()))
                .thenReturn(List.of(10L, 20L));

        service.configure(new RoleDataScopeConfigDTO(
                1L,
                RoleDataScopeType.CUSTOM,
                List.of(10L, 20L)
        ));

        verify(roleDataScopeConfigMapper).insertScope(1L, RoleDataScopeType.CUSTOM);
        verify(roleDataScopeConfigMapper).deleteOrgBindingsByRoleId(1L);
        verify(roleDataScopeConfigMapper).insertOrgBinding(1L, 10L);
        verify(roleDataScopeConfigMapper).insertOrgBinding(1L, 20L);
    }

    @Test
    void rejectsDuplicateOrganizationIdsBeforeAnyWrite() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.configure(new RoleDataScopeConfigDTO(
                        1L,
                        RoleDataScopeType.CUSTOM,
                        List.of(10L, 10L)
                ))
        );

        assertEquals("指定组织存在重复绑定", exception.getMessage());
        verify(roleDataScopeConfigMapper, never()).insertScope(anyLong(), any());
        verify(roleDataScopeConfigMapper, never()).deleteOrgBindingsByRoleId(anyLong());
    }

    @Test
    void rejectsExistingOrganizationBindingBeforeInsert() {
        SysRoleDataScope scope = new SysRoleDataScope();
        scope.setScopeType(RoleDataScopeType.CUSTOM);
        when(roleDataScopeConfigMapper.selectScopeByRoleId(1L)).thenReturn(scope);
        when(roleDataScopeConfigMapper.selectActiveOrgIdsByIds(List.of(10L)))
                .thenReturn(List.of(10L));
        when(roleDataScopeConfigMapper.countOrgBinding(1L, 10L)).thenReturn(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bindOrg(1L, 10L)
        );

        assertEquals("角色已绑定该组织", exception.getMessage());
        verify(roleDataScopeConfigMapper, never()).insertOrgBinding(1L, 10L);
    }

    @Test
    void convertsUniqueConstraintRaceToTheSameDuplicateBindingError() {
        SysRoleDataScope scope = new SysRoleDataScope();
        scope.setScopeType(RoleDataScopeType.CUSTOM);
        when(roleDataScopeConfigMapper.selectScopeByRoleId(1L)).thenReturn(scope);
        when(roleDataScopeConfigMapper.selectActiveOrgIdsByIds(List.of(10L)))
                .thenReturn(List.of(10L));
        when(roleDataScopeConfigMapper.countOrgBinding(1L, 10L)).thenReturn(0);
        doThrow(new DuplicateKeyException("uk_role_org"))
                .when(roleDataScopeConfigMapper)
                .insertOrgBinding(1L, 10L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bindOrg(1L, 10L)
        );

        assertEquals("角色已绑定该组织", exception.getMessage());
    }

    private SysRole activeRole(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(1);
        return role;
    }
}

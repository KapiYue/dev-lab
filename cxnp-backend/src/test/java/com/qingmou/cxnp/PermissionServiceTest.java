
package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.SysPermission;
import com.qingmou.cxnp.mapper.SysPermissionMapper;
import com.qingmou.cxnp.service.impl.PermissionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.qingmou.cxnp.context.UserContext;
import org.junit.jupiter.api.AfterEach;

class PermissionServiceTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        permissionService =
                new PermissionServiceImpl(sysPermissionMapper);
    }

    // 1. 验证有列表权限
    @Test
    void testHasListPermission() {

        SysPermission permission = new SysPermission();
        permission.setPermissionCode("party:member:list");

        when(sysPermissionMapper.selectByUserId(1L))
                .thenReturn(List.of(permission));

        boolean result = permissionService.hasPermission(
                1L,
                "party:member:list"
        );

        assertTrue(result);
    }

    // 2. 验证没有详情权限
    @Test
    void testWithoutDetailPermission() {

        SysPermission permission = new SysPermission();
        permission.setPermissionCode("party:member:list");

        when(sysPermissionMapper.selectByUserId(2L))
                .thenReturn(List.of(permission));

        boolean result = permissionService.hasPermission(
                2L,
                "party:member:detail"
        );

        assertFalse(result);
    }

    // 3. 验证没有任何权限
    @Test
    void testWithoutAnyPermission() {

        when(sysPermissionMapper.selectByUserId(3L))
                .thenReturn(List.of());

        boolean result = permissionService.hasPermission(
                3L,
                "party:member:list"
        );

        assertFalse(result);
    }

    // 4. 验证无权限时抛出异常
    @Test
    void testCheckPermissionDenied() {

        when(sysPermissionMapper.selectByUserId(2L))
                .thenReturn(List.of());

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> permissionService.checkPermission(
                        2L,
                        "party:member:detail"
                )
        );

        assertEquals(
                "无功能权限：party:member:detail",
                exception.getMessage()
        );
    }


    @AfterEach
    void tearDown() {
        // 每次测试结束清理当前线程的用户上下文
        UserContext.clear();
    }

    // 5. 已登录且有功能权限
    @Test
    void testCurrentUserWithPermissions() {

        // 模拟当前登录用户 ID
        UserContext.setUserId(1L);

        SysPermission permission = new SysPermission();
        permission.setPermissionCode("party:member:list");

        when(sysPermissionMapper.selectByUserId(1L))
                .thenReturn(List.of(permission));

        List<SysPermission> result =
                permissionService.getCurrentUserPermissions();

        assertEquals(1, result.size());

        assertEquals(
                "party:member:list",
                result.get(0).getPermissionCode()
        );

        verify(sysPermissionMapper).selectByUserId(1L);
    }

    // 6. 已登录但没有功能权限
    @Test
    void testCurrentUserWithoutPermissions() {

        UserContext.setUserId(2L);

        when(sysPermissionMapper.selectByUserId(2L))
                .thenReturn(List.of());

        List<SysPermission> result =
                permissionService.getCurrentUserPermissions();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(sysPermissionMapper).selectByUserId(2L);
    }

    // 7. 未登录用户
    @Test
    void testCurrentUserNotLoggedIn() {

        UserContext.clear();

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> permissionService.getCurrentUserPermissions()
        );

        assertEquals(
                "用户未登录",
                exception.getMessage()
        );

        // 未登录时不应该查询数据库
        verifyNoInteractions(sysPermissionMapper);
    }


    // 验证 UserContext 可以正确清理
    @Test
    void testUserContextCleanup() {

        // 1. 模拟当前登录用户
        UserContext.setUserId(1L);

        // 2. 确认用户 ID 已写入
        assertEquals(1L, UserContext.getUserId());

        // 3. 清理当前线程的用户信息
        UserContext.clear();

        // 4. 确认用户 ID 已清除
        assertNull(UserContext.getUserId());
    }

}

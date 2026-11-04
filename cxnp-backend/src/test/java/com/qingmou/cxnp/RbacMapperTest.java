
package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.SysRole;
import com.qingmou.cxnp.entity.SysPermission;
import com.qingmou.cxnp.mapper.SysRoleMapper;
import com.qingmou.cxnp.mapper.SysPermissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RbacMapperTest {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Test
    void testSelectAllRoles() {
        List<SysRole> roles = sysRoleMapper.selectAll();

        assertNotNull(roles);
        assertTrue(roles.stream()
                .anyMatch(r -> "ADMIN".equals(r.getRoleCode())));
        assertTrue(roles.stream()
                .anyMatch(r -> "USER".equals(r.getRoleCode())));
    }

    @Test
    void testSelectByRoleCode() {
        SysRole role = sysRoleMapper.selectByRoleCode("ADMIN");

        assertNotNull(role);
        assertEquals("ADMIN", role.getRoleCode());
    }

    @Test
    void testSelectAllPermissions() {
        List<SysPermission> permissions =
                sysPermissionMapper.selectAll();

        assertNotNull(permissions);
        assertTrue(permissions.stream()
                .anyMatch(p -> "party:member:list"
                        .equals(p.getPermissionCode())));
        assertTrue(permissions.stream()
                .anyMatch(p -> "party:member:detail"
                        .equals(p.getPermissionCode())));
    }

    @Test
    void testSelectByPermissionCode() {
        SysPermission permission =
                sysPermissionMapper.selectByPermissionCode(
                        "party:member:list"
                );

        assertNotNull(permission);
        assertEquals(
                "party:member:list",
                permission.getPermissionCode()
        );
    }

    @Test
    void testSelectByUserIdWithoutRoles() {
        // 使用数据库中不存在的用户 ID
        Long userId = -1L;

        List<SysPermission> permissions =
                sysPermissionMapper.selectByUserId(userId);

        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }
}

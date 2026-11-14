package com.qingmou.cxnp;

import com.qingmou.cxnp.constant.PermissionConstants;
import com.qingmou.cxnp.entity.SysPermission;
import com.qingmou.cxnp.mapper.SysPermissionMapper;
import com.qingmou.cxnp.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the session-revoke permission initialization script is
 * repeatable on the test database and that the normal RBAC query path grants
 * the permission to an ADMIN user.
 */
@SpringBootTest
@Transactional
class SessionRevokePermissionInitializationIntegrationTest {

    private static final String SCRIPT_PATH =
            "sql/20_auth_session_revoke_permission_init.sql";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private PermissionService permissionService;

    @Test
    void initializesPermissionIdempotentlyAndGrantsItToAdmin() throws Exception {
        executeInitializationScript();
        executeInitializationScript();

        SysPermission permission = sysPermissionMapper.selectByPermissionCode(
                PermissionConstants.RBAC_USER_SESSION_REVOKE);
        assertNotNull(permission);
        assertEquals(PermissionConstants.RBAC_USER_SESSION_REVOKE,
                permission.getPermissionCode());
        assertEquals(1, count("""
                SELECT COUNT(*)
                FROM sys_permission
                WHERE permission_code = ?
                """, PermissionConstants.RBAC_USER_SESSION_REVOKE));
        assertEquals(1, count("""
                SELECT COUNT(*)
                FROM sys_role_permission rp
                INNER JOIN sys_role role ON role.id = rp.role_id
                WHERE role.role_code = 'ADMIN'
                  AND rp.permission_id = ?
                """, permission.getId()));

        Long adminUserId = createAdminUser();
        assertTrue(permissionService.hasPermission(adminUserId,
                PermissionConstants.RBAC_USER_SESSION_REVOKE));
    }

    private void executeInitializationScript() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(connection,
                new FileSystemResource(SCRIPT_PATH));
    }

    private Long createAdminUser() {
        String username = "sr-" + UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO sys_user (username, password, status)
                VALUES (?, 'not-used-by-this-test', 1)
                """, username);
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        jdbcTemplate.update("""
                INSERT INTO sys_user_role (user_id, role_id)
                SELECT ?, id
                FROM sys_role
                WHERE role_code = 'ADMIN'
                """, userId);
        return userId;
    }

    private int count(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, Integer.class, arguments);
    }
}
